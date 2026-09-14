package com.threadly.common.page;

import java.util.List;

/**
 * One page of a cursor-paginated collection.
 *
 * @param nextCursor token to pass back for the following page, or {@code null} at the end
 */
public record CursorPage<T>(List<T> items, String nextCursor) {

	public boolean hasMore() {
		return nextCursor != null;
	}

	public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
		return new CursorPage<>(items, nextCursor);
	}
}
