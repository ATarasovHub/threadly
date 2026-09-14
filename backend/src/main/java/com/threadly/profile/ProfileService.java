package com.threadly.profile;

import com.threadly.block.BlockService;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.follow.FollowRepository;
import com.threadly.post.PostRepository;
import com.threadly.profile.dto.ProfileResponse;
import com.threadly.profile.dto.UpdateProfileRequest;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final CurrentUserService currentUserService;
	private final UserRepository users;
	private final FollowRepository follows;
	private final BlockService blockService;
	private final PostRepository posts;

	/**
	 * Looks up a profile by handle, case-insensitively, so {@code /andrii} and {@code /Andrii}
	 * reach the same account.
	 */
	@Transactional(readOnly = true)
	public ProfileResponse findByUsername(String username) {
		User user = users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));
		User viewer = currentUserService.require();
		// A blocked account is reported as missing rather than forbidden: acknowledging it exists
		// would tell each side exactly who blocked whom.
		if (blockService.isBlockedBetween(viewer.getId(), user.getId())) {
			throw new ResourceNotFoundException("No account with handle @" + username);
		}
		return describe(user, viewer);
	}

	@Transactional
	public ProfileResponse updateOwnProfile(UpdateProfileRequest request) {
		User user = currentUserService.require();
		user.updateProfile(
				request.displayName(),
				request.bio(),
				request.location(),
				request.website(),
				request.avatarUrl(),
				request.bannerUrl());
		// The entity is managed inside this transaction, so the update is flushed on commit.
		return describe(user, user);
	}

	/**
	 * Builds the response for {@code subject} as seen by {@code viewer}.
	 *
	 * <p>Counts are queried on read rather than kept in denormalised columns: correctness first,
	 * and these are cheap index-only counts. If a hot profile ever makes that the bottleneck, the
	 * place to add cached counters is here.
	 */
	private ProfileResponse describe(User subject, User viewer) {
		ProfileResponse.ProfileStats stats = new ProfileResponse.ProfileStats(
				follows.countByFolloweeId(subject.getId()),
				follows.countByFollowerId(subject.getId()),
				posts.countVisibleByAuthorId(subject.getId()));

		// One's own profile has no relationship to report.
		ProfileResponse.Relationship relationship = subject.getId().equals(viewer.getId())
				? null
				: new ProfileResponse.Relationship(
						follows.existsByFollowerIdAndFolloweeId(viewer.getId(), subject.getId()),
						follows.existsByFollowerIdAndFolloweeId(subject.getId(), viewer.getId()));

		return ProfileResponse.from(subject, stats, relationship);
	}
}
