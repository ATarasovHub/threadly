package com.threadly.common.page;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.data.domain.Limit;

/**
 * Turns two repository queries — "first page" and "page after a cursor" — into a {@link CursorPage}.
 *
 * <p>The trick is fetching one row beyond the requested size: if it comes back there is another
 * page, and the last kept row supplies the next cursor. That avoids the separate COUNT query an
 * offset-paginated API needs.
 */
public final class CursorPaging {

	private CursorPaging() {
	}

	/** Maps rows one at a time; use {@link #pageOfMany} when the mapping needs the whole page. */
	public static <E, R> CursorPage<R> page(
			String encodedCursor,
			int limit,
			Function<Limit, List<E>> firstPage,
			BiFunction<Cursor, Limit, List<E>> pageAfter,
			Function<E, Cursor> cursorOf,
			Function<E, R> toResponse) {

		return pageOfMany(encodedCursor, limit, firstPage, pageAfter, cursorOf,
				rows -> rows.stream().map(toResponse).toList());
	}

	/**
	 * Maps the page as a batch, so responses that need extra data — counters, viewer state — can
	 * load it with a fixed number of queries instead of one per row.
	 */
	public static <E, R> CursorPage<R> pageOfMany(
			String encodedCursor,
			int limit,
			Function<Limit, List<E>> firstPage,
			BiFunction<Cursor, Limit, List<E>> pageAfter,
			Function<E, Cursor> cursorOf,
			Function<List<E>, List<R>> toResponses) {

		Limit window = Limit.of(limit + 1);
		List<E> rows = encodedCursor == null
				? firstPage.apply(window)
				: pageAfter.apply(Cursor.decode(encodedCursor), window);

		boolean hasMore = rows.size() > limit;
		List<E> visible = hasMore ? rows.subList(0, limit) : rows;
		String nextCursor = hasMore ? cursorOf.apply(visible.getLast()).encode() : null;

		return CursorPage.of(toResponses.apply(visible), nextCursor);
	}
}
