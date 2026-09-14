package com.threadly.follow.dto;

import com.threadly.user.User;

/** Compact account view used in follower and following lists. */
public record UserSummaryResponse(
		Long id,
		String username,
		String displayName,
		String bio,
		String avatarUrl) {

	public static UserSummaryResponse from(User user) {
		return new UserSummaryResponse(
				user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(), user.getAvatarUrl());
	}
}
