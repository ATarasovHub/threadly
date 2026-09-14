package com.threadly.bookmark;

import com.threadly.block.BlockService;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.post.Post;
import com.threadly.post.PostAssembler;
import com.threadly.post.PostRepository;
import com.threadly.post.dto.PostResponse;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

	private final BookmarkRepository bookmarks;
	private final PostRepository posts;
	private final PostAssembler assembler;
	private final CurrentUserService currentUserService;
	private final BlockService blockService;

	@Transactional
	public void save(Long postId) {
		User me = currentUserService.require();
		Post post = requireVisible(postId, me);

		if (bookmarks.existsByPostIdAndUserId(postId, me.getId())) {
			return;
		}
		try {
			bookmarks.save(Bookmark.of(post, me));
		}
		catch (DataIntegrityViolationException e) {
			// Concurrent duplicate; the post is saved either way.
		}
	}

	@Transactional
	public void remove(Long postId) {
		User me = currentUserService.require();
		bookmarks.deleteByPostIdAndUserId(postId, me.getId());
	}

	/** The caller's saved posts, most recently saved first. */
	@Transactional(readOnly = true)
	public CursorPage<PostResponse> saved(String encodedCursor, int limit) {
		User me = currentUserService.require();
		return CursorPaging.pageOfMany(
				encodedCursor,
				limit,
				window -> bookmarks.findSaved(me.getId(), window),
				(position, window) -> bookmarks.findSavedBefore(
						me.getId(), position.createdAt(), position.id(), window),
				// Paged on the bookmark's timestamp, not the post's: the list is ordered by when
				// it was saved.
				bookmark -> new Cursor(bookmark.getCreatedAt(), bookmark.getId()),
				page -> assembler.toResponses(page.stream().map(Bookmark::getPost).toList(), me));
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
