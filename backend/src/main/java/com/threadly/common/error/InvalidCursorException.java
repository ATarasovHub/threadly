package com.threadly.common.error;

/** Thrown when a pagination cursor cannot be decoded. */
public class InvalidCursorException extends RuntimeException {

	public InvalidCursorException(String message) {
		super(message);
	}
}
