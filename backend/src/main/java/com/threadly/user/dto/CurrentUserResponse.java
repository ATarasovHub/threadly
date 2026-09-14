package com.threadly.user.dto;

import com.threadly.user.Role;
import com.threadly.user.User;
import java.time.Instant;

/** What an account may see about itself, including fields hidden from everyone else. */
public record CurrentUserResponse(
		Long id,
		String username,
		String displayName,
		String email,
		Role role,
		Instant createdAt) {

	public static CurrentUserResponse from(User user) {
		return new CurrentUserResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt());
	}
}
