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
		ViewerState viewer,
		ParentRef inReplyTo,
		RepostedPost repostOf) {

	public static PostResponse of(Post post, Metrics metrics, ViewerState viewer) {
		ParentRef inReplyTo = post.isReply()
				? new ParentRef(post.getParent().getId(), post.getParent().getAuthor().getUsername())
				: null;
		// Only one level deep: a repost of a repost still shows the post that was reposted, not
		// the whole chain, which is what clients render and what stops the response nesting away.
		RepostedPost repostOf = post.isRepost() ? RepostedPost.from(post.getRepostOf()) : null;
		return new PostResponse(
				post.getId(),
				post.getContent(),
				PostAuthor.from(post),
				post.getEditedAt() != null,
				post.getCreatedAt(),
				metrics,
				viewer,
				inReplyTo,
				repostOf);
	}

	public record PostAuthor(Long id, String username, String displayName, String avatarUrl) {

		static PostAuthor from(Post post) {
			var author = post.getAuthor();
			return new PostAuthor(author.getId(), author.getUsername(), author.getDisplayName(),
					author.getAvatarUrl());
		}
	}

	public record Metrics(long likes, long replies, long reposts) {
	}

	/** Enough of the parent for a client to render "Replying to @andrii" and link to it. */
	public record ParentRef(Long id, String authorUsername) {
	}

	/** The post that was reposted or quoted, flattened so responses cannot nest indefinitely. */
	public record RepostedPost(Long id, String content, PostAuthor author, Instant createdAt) {

		static RepostedPost from(Post original) {
			return new RepostedPost(
					original.getId(),
					original.getContent(),
					PostAuthor.from(original),
					original.getCreatedAt());
		}
	}

	public record ViewerState(boolean liked, boolean bookmarked, boolean reposted) {
	}
}
