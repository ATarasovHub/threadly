package com.threadly.auth.dto;

import com.threadly.user.dto.UserResponse;

/**
 * @param expiresIn lifetime of the access token in seconds, so clients can refresh ahead of expiry
 */
public record AuthenticationResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		UserResponse user) {

	public static AuthenticationResponse bearer(String accessToken, long expiresIn, UserResponse user) {
		return new AuthenticationResponse(accessToken, "Bearer", expiresIn, user);
	}
}
