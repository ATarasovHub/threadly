package com.threadly.follow;

import com.threadly.common.error.BadRequestException;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.follow.dto.UserSummaryResponse;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

	private final FollowRepository follows;
	private final UserRepository users;
	private final CurrentUserService currentUserService;

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
		if (follows.existsByFollowerIdAndFolloweeId(me.getId(), target.getId())) {
			return;
		}

		try {
			follows.save(Follow.of(me, target));
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
		return page(
				encodedCursor,
				limit,
				window -> follows.findFollowers(target.getId(), window),
				(cursor, window) -> follows.findFollowersBefore(
						target.getId(), cursor.createdAt(), cursor.id(), window),
				Follow::getFollower);
	}

	@Transactional(readOnly = true)
	public CursorPage<UserSummaryResponse> followingOf(String username, String encodedCursor, int limit) {
		User target = requireAccount(username);
		return page(
				encodedCursor,
				limit,
				window -> follows.findFollowing(target.getId(), window),
				(cursor, window) -> follows.findFollowingBefore(
						target.getId(), cursor.createdAt(), cursor.id(), window),
				Follow::getFollowee);
	}

	/**
	 * Shared cursor-paging shape for both directions of the graph: fetch one row beyond the page
	 * so the presence of a next page is known without a second query.
	 */
	private CursorPage<UserSummaryResponse> page(
			String encodedCursor,
			int limit,
			Function<Limit, List<Follow>> firstPage,
			java.util.function.BiFunction<Cursor, Limit, List<Follow>> nextPage,
			Function<Follow, User> side) {

		Limit window = Limit.of(limit + 1);
		List<Follow> rows = encodedCursor == null
				? firstPage.apply(window)
				: nextPage.apply(Cursor.decode(encodedCursor), window);

		boolean hasMore = rows.size() > limit;
		List<Follow> visible = hasMore ? rows.subList(0, limit) : rows;
		String nextCursor = hasMore
				? new Cursor(visible.getLast().getCreatedAt(), visible.getLast().getId()).encode()
				: null;

		return CursorPage.of(
				visible.stream().map(side).map(UserSummaryResponse::from).toList(), nextCursor);
	}

	private User requireAccount(String username) {
		return users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));
	}
}
