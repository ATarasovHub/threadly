package com.threadly.common.error;

/** Thrown when a uniqueness rule (handle, email, ...) would be violated. */
public class DuplicateResourceException extends RuntimeException {

	private final String field;

	public DuplicateResourceException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
