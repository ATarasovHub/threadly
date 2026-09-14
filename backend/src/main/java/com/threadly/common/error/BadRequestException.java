package com.threadly.common.error;

/** Thrown when a request is well-formed but asks for something the domain does not allow. */
public class BadRequestException extends RuntimeException {

	public BadRequestException(String message) {
		super(message);
	}
}
