package com.threadly.auth.refresh;

import org.springframework.security.core.AuthenticationException;

/**
 * The session could not be renewed: the token is missing, unknown, expired, or was already
 * rotated away. Kept apart from bad login credentials so clients can tell "sign in again" from
 * "that password was wrong".
 */
public class InvalidRefreshTokenException extends AuthenticationException {

	public InvalidRefreshTokenException(String message) {
		super(message);
	}
}
