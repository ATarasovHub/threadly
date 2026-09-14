package com.threadly.post.dto;

import com.threadly.post.Post;
import java.time.Instant;

/**
 * @param author  a compact author view, so a timeline renders without a second request
 * @param edited  whether the text has been changed since it was posted
 */
public record PostResponse(
		Long id,
		String content,
		PostAuthor author,
		boolean edited,
		Instant createdAt) {

	public static PostResponse from(Post post) {
		return new PostResponse(
				post.getId(),
				post.getContent(),
				PostAuthor.from(post),
				post.getEditedAt() != null,
				post.getCreatedAt());
	}

	public record PostAuthor(Long id, String username, String displayName, String avatarUrl) {

		static PostAuthor from(Post post) {
			var author = post.getAuthor();
			return new PostAuthor(author.getId(), author.getUsername(), author.getDisplayName(),
					author.getAvatarUrl());
		}
	}
}
