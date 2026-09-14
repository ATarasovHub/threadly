package com.threadly.follow;

import com.threadly.block.BlockService;
import com.threadly.common.error.BadRequestException;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.follow.dto.UserSummaryResponse;
import com.threadly.notification.NotificationEvents;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

	private final FollowRepository follows;
	private final UserRepository users;
	private final CurrentUserService currentUserService;
	private final BlockService blockService;
	private final ApplicationEventPublisher events;

	/**
	 * Follows an account.
	 *
	 * <p>Idempotent: following twice leaves the same single edge, because "follows" is a state
	 * rather than an event. The unique index is what makes that safe when two requests race.
	 */
	@Transactional
	public void follow(String username) {
		User me = currentUserService.require();
		User target = requireAccount(username);

		if (target.getId().equals(me.getId())) {
			throw new BadRequestException("You cannot follow yourself.");
		}
		if (blockService.isBlockedBetween(me.getId(), target.getId())) {
			throw new ResourceNotFoundException("No account with handle @" + username);
		}
		if (follows.existsByFollowerIdAndFolloweeId(me.getId(), target.getId())) {
			return;
		}

		try {
			follows.save(Follow.of(me, target));
			events.publishEvent(new NotificationEvents.Followed(me.getId(), target.getId()));
		}
		catch (DataIntegrityViolationException e) {
			// Another request created the same edge in between; the desired state already holds.
		}
	}

	/** Unfollows an account. Also idempotent: unfollowing someone you do not follow is a no-op. */
	@Transactional
	public void unfollow(String username) {
		User me = currentUserService.require();
		User target = requireAccount(username);
		follows.deleteByFollowerIdAndFolloweeId(me.getId(), target.getId());
	}

	@Transactional(readOnly = true)
	public CursorPage<UserSummaryResponse> followersOf(String username, String encodedCursor, int limit) {
		User target = requireAccount(username);
		return CursorPaging.page(
				encodedCursor,
				limit,
				window -> follows.findFollowers(target.getId(), window),
				(position, window) -> follows.findFollowersBefore(
						target.getId(), position.createdAt(), position.id(), window),
				FollowService::positionOf,
				edge -> UserSummaryResponse.from(edge.getFollower()));
	}

	@Transactional(readOnly = true)
	public CursorPage<UserSummaryResponse> followingOf(String username, String encodedCursor, int limit) {
		User target = requireAccount(username);
		return CursorPaging.page(
				encodedCursor,
				limit,
				window -> follows.findFollowing(target.getId(), window),
				(position, window) -> follows.findFollowingBefore(
						target.getId(), position.createdAt(), position.id(), window),
				FollowService::positionOf,
				edge -> UserSummaryResponse.from(edge.getFollowee()));
	}

	private static Cursor positionOf(Follow edge) {
		return new Cursor(edge.getCreatedAt(), edge.getId());
	}

	private User requireAccount(String username) {
		return users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));
	}
}
