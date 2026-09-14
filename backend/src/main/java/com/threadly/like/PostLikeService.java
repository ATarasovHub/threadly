package com.threadly.like;

import com.threadly.block.BlockService;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.notification.NotificationEvents;
import com.threadly.post.Post;
import com.threadly.post.PostRepository;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

	private final PostLikeRepository likes;
	private final PostRepository posts;
	private final CurrentUserService currentUserService;
	private final BlockService blockService;
	private final ApplicationEventPublisher events;

	/** Idempotent, like following: a like is a state, and a retried request must not double it. */
	@Transactional
	public void like(Long postId) {
		User me = currentUserService.require();
		Post post = requireVisible(postId, me);

		if (likes.existsByPostIdAndUserId(postId, me.getId())) {
			return;
		}
		try {
			likes.save(PostLike.of(post, me));
			events.publishEvent(new NotificationEvents.PostLiked(
					me.getId(), post.getAuthor().getId(), post.getId()));
		}
		catch (DataIntegrityViolationException e) {
			// Concurrent duplicate; the unique index already holds the desired state.
		}
	}

	@Transactional
	public void unlike(Long postId) {
		User me = currentUserService.require();
		requireVisible(postId, me);
		likes.deleteByPostIdAndUserId(postId, me.getId());
	}

	private Post requireVisible(Long postId, User viewer) {
		Post post = posts.findVisibleById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("No post with id " + postId));
		if (blockService.isBlockedBetween(viewer.getId(), post.getAuthor().getId())) {
			throw new ResourceNotFoundException("No post with id " + postId);
		}
		return post;
	}
}
