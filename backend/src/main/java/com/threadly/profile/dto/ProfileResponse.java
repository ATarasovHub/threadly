package com.threadly.profile.dto;

import com.threadly.user.User;
import java.time.Instant;

/** A profile as anyone may see it: the handle, the decoration, and when the account joined. */
public record ProfileResponse(
		Long id,
		String username,
		String displayName,
		String bio,
		String location,
		String website,
		String avatarUrl,
		String bannerUrl,
		Instant joinedAt) {

	public static ProfileResponse from(User user) {
		return new ProfileResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getBio(),
				user.getLocation(),
				user.getWebsite(),
				user.getAvatarUrl(),
				user.getBannerUrl(),
				user.getCreatedAt());
	}
}
