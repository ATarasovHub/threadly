package com.threadly.post;

import com.threadly.block.BlockService;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.notification.NotificationEvents;
import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.dto.UpdatePostRequest;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository posts;
	private final UserRepository users;
	private final CurrentUserService currentUserService;
	private final BlockService blockService;
	private final PostAssembler assembler;
	private final ApplicationEventPublisher events;

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		User me = currentUserService.require();
		Post post = posts.save(Post.write(me, request.content()));
		return assembler.toResponse(post, me);
	}

	/**
	 * Reposts a post.
	 *
	 * <p>Idempotent, and deliberately so: the button is a toggle, and a retry must not create a
	 * second repost. A partial unique index enforces that when requests race.
	 */
	@Transactional
	public void repost(Long originalId) {
		User me = currentUserService.require();
		Post original = requireVisible(originalId);
		requireNotBlocked(original.getAuthor().getId(), "No post with id " + originalId);

		if (posts.existsByAuthorIdAndRepostOfIdAndContentIsNullAndDeletedAtIsNull(me.getId(), originalId)) {
			return;
		}
		try {
			posts.save(Post.repost(original, me));
			events.publishEvent(new NotificationEvents.PostReposted(
					me.getId(), original.getAuthor().getId(), original.getId(), false));
		}
		catch (DataIntegrityViolationException e) {
			// Concurrent duplicate; the post is reposted either way.
		}
	}

	@Transactional
	public void undoRepost(Long originalId) {
		User me = currentUserService.require();
		posts.deleteByAuthorIdAndRepostOfIdAndContentIsNull(me.getId(), originalId);
	}

	/**
	 * Quotes a post: the caller's own words with the original attached.
	 *
	 * <p>Unlike a plain repost this is not idempotent — quoting the same post twice with different
	 * commentary is a normal thing to do.
	 */
	@Transactional
	public PostResponse quote(Long originalId, CreatePostRequest request) {
		User me = currentUserService.require();
		Post original = requireVisible(originalId);
		requireNotBlocked(original.getAuthor().getId(), "No post with id " + originalId);

		Post quote = posts.save(Post.quote(original, me, request.content()));
		events.publishEvent(new NotificationEvents.PostReposted(
				me.getId(), original.getAuthor().getId(), original.getId(), true));
		return assembler.toResponse(quote, me);
	}

	/** Publishes a reply to an existing post. */
	@Transactional
	public PostResponse reply(Long parentId, CreatePostRequest request) {
		User me = currentUserService.require();
		Post parent = requireVisible(parentId);
		requireNotBlocked(parent.getAuthor().getId(), "No post with id " + parentId);

		Post reply = posts.save(Post.replyTo(parent, me, request.content()));
		events.publishEvent(new NotificationEvents.PostReplied(
				me.getId(), parent.getAuthor().getId(), reply.getId()));
		return assembler.toResponse(reply, me);
	}

	/**
	 * A thread: the replies to one post.
	 *
	 * <p>Oldest first, unlike every other listing — a conversation is read from its start.
	 */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> repliesTo(Long parentId, String encodedCursor, int limit) {
		User viewer = currentUserService.require();
		Post parent = requireVisible(parentId);
		requireNotBlocked(parent.getAuthor().getId(), "No post with id " + parentId);

		return CursorPaging.pageOfMany(
				encodedCursor,
				limit,
				window -> posts.findReplies(parentId, window),
				(position, window) -> posts.findRepliesAfter(
						parentId, position.createdAt(), position.id(), window),
				post -> new Cursor(post.getCreatedAt(), post.getId()),
				page -> assembler.toResponses(page, viewer));
	}

	@Transactional(readOnly = true)
	public PostResponse findById(Long id) {
		Post post = requireVisible(id);
		User me = currentUserService.require();
		requireNotBlocked(post.getAuthor().getId(), "No post with id " + id);
		return assembler.toResponse(post, me);
	}

	@Transactional
	public PostResponse update(Long id, UpdatePostRequest request) {
		Post post = requireOwned(id);
		post.edit(request.content());
		return assembler.toResponse(post, currentUserService.require());
	}

	@Transactional
	public void delete(Long id) {
		requireOwned(id).delete();
	}

	/** An author's timeline, newest first. */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> timelineOf(String username, String encodedCursor, int limit) {
		User author = users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));

		requireNotBlocked(author.getId(), "No account with handle @" + username);
		User viewer = currentUserService.require();

		return CursorPaging.pageOfMany(
				encodedCursor,
				limit,
				window -> posts.findAuthorTimeline(author.getId(), window),
				(position, window) -> posts.findAuthorTimelineBefore(
						author.getId(), position.createdAt(), position.id(), window),
				post -> new Cursor(post.getCreatedAt(), post.getId()),
				page -> assembler.toResponses(page, viewer));
	}

	private void requireNotBlocked(Long otherAccountId, String message) {
		if (blockService.isBlockedBetween(currentUserService.require().getId(), otherAccountId)) {
			throw new ResourceNotFoundException(message);
		}
	}

	private Post requireVisible(Long id) {
		return posts.findVisibleById(id)
				.orElseThrow(() -> new ResourceNotFoundException("No post with id " + id));
	}

	private Post requireOwned(Long id) {
		Post post = requireVisible(id);
		if (!post.isAuthoredBy(currentUserService.require())) {
			// Deliberately 403 rather than 404: the post exists and the caller can already see it.
			throw new AccessDeniedException("Only the author can change this post");
		}
		return post;
	}
}
