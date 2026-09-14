package com.threadly.user.dto;

import com.threadly.user.User;
import java.time.Instant;

/** Public view of an account. Deliberately omits email and anything credential-related. */
public record UserResponse(Long id, String username, String displayName, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
	}
}
