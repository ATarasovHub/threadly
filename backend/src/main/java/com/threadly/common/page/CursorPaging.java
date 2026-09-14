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

	public static <E, R> CursorPage<R> page(
			String encodedCursor,
			int limit,
			Function<Limit, List<E>> firstPage,
			BiFunction<Cursor, Limit, List<E>> pageAfter,
			Function<E, Cursor> cursorOf,
			Function<E, R> toResponse) {

		Limit window = Limit.of(limit + 1);
		List<E> rows = encodedCursor == null
				? firstPage.apply(window)
				: pageAfter.apply(Cursor.decode(encodedCursor), window);

		boolean hasMore = rows.size() > limit;
		List<E> visible = hasMore ? rows.subList(0, limit) : rows;
		String nextCursor = hasMore ? cursorOf.apply(visible.getLast()).encode() : null;

		return CursorPage.of(visible.stream().map(toResponse).toList(), nextCursor);
	}
}
