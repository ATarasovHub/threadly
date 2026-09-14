package com.threadly.common.page;

import com.threadly.common.error.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Position in a timeline ordered by {@code (createdAt desc, id desc)}.
 *
 * <p>Encoded opaquely so clients treat it as a token rather than something to build themselves,
 * which leaves the ordering free to change later. It is not a security boundary: the contents are
 * only a timestamp and an id, both of which the client already has.
 *
 * @param createdAt timestamp of the last row of the previous page
 * @param id        id of that row, which breaks ties between posts sharing a timestamp
 */
public record Cursor(Instant createdAt, Long id) {

	public String encode() {
		String raw = "%d.%d.%d".formatted(createdAt.getEpochSecond(), createdAt.getNano(), id);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	public static Cursor decode(String encoded) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
			String[] parts = raw.split("\\.");
			if (parts.length != 3) {
				throw new IllegalArgumentException("expected three parts, got " + parts.length);
			}
			Instant createdAt = Instant.ofEpochSecond(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
			return new Cursor(createdAt, Long.parseLong(parts[2]));
		}
		catch (IllegalArgumentException | ArithmeticException e) {
			throw new InvalidCursorException("The cursor is not a value this API issued.");
		}
	}
}
