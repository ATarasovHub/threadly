package com.threadly.feed;

import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.post.Post;
import com.threadly.post.PostRepository;
import com.threadly.post.dto.PostResponse;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

	private final PostRepository posts;
	private final CurrentUserService currentUserService;

	/** Posts by the accounts the caller follows, and the caller's own, newest first. */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> following(String cursor, int limit) {
		User viewer = currentUserService.require();
		return CursorPaging.page(
				cursor,
				limit,
				window -> posts.findFollowingFeed(viewer.getId(), window),
				(position, window) -> posts.findFollowingFeedBefore(
						viewer.getId(), position.createdAt(), position.id(), window),
				FeedService::positionOf,
				PostResponse::from);
	}

	/**
	 * Everything on Threadly, newest first.
	 *
	 * <p>Deliberately not personalised yet. Calling a recency list "For You" would be a lie, and
	 * there are no engagement signals to rank on until likes exist.
	 */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> forYou(String cursor, int limit) {
		User viewer = currentUserService.require();
		return CursorPaging.page(
				cursor,
				limit,
				window -> posts.findGlobalFeed(viewer.getId(), window),
				(position, window) -> posts.findGlobalFeedBefore(
						viewer.getId(), position.createdAt(), position.id(), window),
				FeedService::positionOf,
				PostResponse::from);
	}

	private static Cursor positionOf(Post post) {
		return new Cursor(post.getCreatedAt(), post.getId());
	}
}
