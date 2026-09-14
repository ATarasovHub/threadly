package com.threadly.post;

import com.threadly.block.BlockService;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.dto.UpdatePostRequest;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
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

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		Post post = Post.write(currentUserService.require(), request.content());
		return PostResponse.from(posts.save(post));
	}

	@Transactional(readOnly = true)
	public PostResponse findById(Long id) {
		Post post = requireVisible(id);
		requireNotBlocked(post.getAuthor().getId(), "No post with id " + id);
		return PostResponse.from(post);
	}

	@Transactional
	public PostResponse update(Long id, UpdatePostRequest request) {
		Post post = requireOwned(id);
		post.edit(request.content());
		return PostResponse.from(post);
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

		return CursorPaging.page(
				encodedCursor,
				limit,
				window -> posts.findAuthorTimeline(author.getId(), window),
				(position, window) -> posts.findAuthorTimelineBefore(
						author.getId(), position.createdAt(), position.id(), window),
				post -> new Cursor(post.getCreatedAt(), post.getId()),
				PostResponse::from);
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
