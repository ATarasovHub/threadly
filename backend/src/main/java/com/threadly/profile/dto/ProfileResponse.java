package com.threadly.profile.dto;

import com.threadly.user.User;
import java.time.Instant;

/**
 * A profile as anyone may see it.
 *
 * @param stats        follower, following and post counts
 * @param relationship how the caller relates to this account; {@code null} on one's own profile
 */
public record ProfileResponse(
		Long id,
		String username,
		String displayName,
		String bio,
		String location,
		String website,
		String avatarUrl,
		String bannerUrl,
		Instant joinedAt,
		ProfileStats stats,
		Relationship relationship) {

	public static ProfileResponse from(User user, ProfileStats stats, Relationship relationship) {
		return new ProfileResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getBio(),
				user.getLocation(),
				user.getWebsite(),
				user.getAvatarUrl(),
				user.getBannerUrl(),
				user.getCreatedAt(),
				stats,
				relationship);
	}

	public record ProfileStats(long followers, long following, long posts) {
	}

	/**
	 * @param following   the caller follows this account
	 * @param followedBy  this account follows the caller, which clients show as "follows you"
	 */
	public record Relationship(boolean following, boolean followedBy) {
	}
}
