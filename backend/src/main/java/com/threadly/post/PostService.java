package com.threadly.post;

import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.dto.UpdatePostRequest;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository posts;
	private final UserRepository users;
	private final CurrentUserService currentUserService;

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		Post post = Post.write(currentUserService.require(), request.content());
		return PostResponse.from(posts.save(post));
	}

	@Transactional(readOnly = true)
	public PostResponse findById(Long id) {
		return PostResponse.from(requireVisible(id));
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

	/**
	 * An author's timeline, newest first.
	 *
	 * <p>One extra row is fetched beyond the requested page size: if it comes back there is
	 * another page, and the extra row supplies the next cursor without a second count query.
	 */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> timelineOf(String username, String encodedCursor, int limit) {
		User author = users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));

		Limit window = Limit.of(limit + 1);
		List<Post> page = encodedCursor == null
				? posts.findAuthorTimeline(author.getId(), window)
				: pageAfter(author, Cursor.decode(encodedCursor), window);

		boolean hasMore = page.size() > limit;
		List<Post> visible = hasMore ? page.subList(0, limit) : page;
		String nextCursor = hasMore ? cursorOf(visible.getLast()) : null;

		return CursorPage.of(visible.stream().map(PostResponse::from).toList(), nextCursor);
	}

	private List<Post> pageAfter(User author, Cursor cursor, Limit window) {
		return posts.findAuthorTimelineBefore(author.getId(), cursor.createdAt(), cursor.id(), window);
	}

	private static String cursorOf(Post post) {
		return new Cursor(post.getCreatedAt(), post.getId()).encode();
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
