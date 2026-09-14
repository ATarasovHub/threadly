package com.threadly.post;

import com.threadly.like.PostLikeRepository;
import com.threadly.post.dto.PostResponse;
import com.threadly.user.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns posts into responses, resolving counters and viewer state for a whole page at once.
 *
 * <p>Every per-post fact is loaded with a fixed number of queries regardless of page size. Doing
 * it per post instead would mean one query per row, which is what makes naive feeds collapse
 * under a long timeline.
 */
@Component
@RequiredArgsConstructor
public class PostAssembler {

	private final PostLikeRepository likes;
	private final PostRepository postRepository;

	public PostResponse toResponse(Post post, User viewer) {
		return toResponses(List.of(post), viewer).getFirst();
	}

	public List<PostResponse> toResponses(List<Post> posts, User viewer) {
		if (posts.isEmpty()) {
			return List.of();
		}

		List<Long> ids = posts.stream().map(Post::getId).toList();

		Map<Long, Long> likeCounts = new HashMap<>();
		for (Object[] row : likes.countByPostIds(ids)) {
			likeCounts.put((Long) row[0], (Long) row[1]);
		}
		Set<Long> likedByViewer = Set.copyOf(likes.findLikedPostIds(viewer.getId(), ids));

		Map<Long, Long> replyCounts = new HashMap<>();
		for (Object[] row : postRepository.countRepliesByParentIds(ids)) {
			replyCounts.put((Long) row[0], (Long) row[1]);
		}

		return posts.stream()
				.map(post -> PostResponse.of(
						post,
						new PostResponse.Metrics(
								likeCounts.getOrDefault(post.getId(), 0L),
								replyCounts.getOrDefault(post.getId(), 0L)),
						new PostResponse.ViewerState(likedByViewer.contains(post.getId()))))
				.toList();
	}
}
