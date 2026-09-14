package com.threadly.post.dto;

import com.threadly.post.Post;
import java.time.Instant;

/**
 * A post as one viewer sees it.
 *
 * @param author  a compact author view, so a timeline renders without a second request
 * @param edited  whether the text has been changed since it was posted
 * @param metrics public counters, the same for everyone
 * @param viewer  what this particular caller has done with the post
 */
public record PostResponse(
		Long id,
		String content,
		PostAuthor author,
		boolean edited,
		Instant createdAt,
		Metrics metrics,
		ViewerState viewer) {

	public static PostResponse of(Post post, Metrics metrics, ViewerState viewer) {
		return new PostResponse(
				post.getId(),
				post.getContent(),
				PostAuthor.from(post),
				post.getEditedAt() != null,
				post.getCreatedAt(),
				metrics,
				viewer);
	}

	public record PostAuthor(Long id, String username, String displayName, String avatarUrl) {

		static PostAuthor from(Post post) {
			var author = post.getAuthor();
			return new PostAuthor(author.getId(), author.getUsername(), author.getDisplayName(),
					author.getAvatarUrl());
		}
	}

	public record Metrics(long likes) {
	}

	public record ViewerState(boolean liked) {
	}
}
