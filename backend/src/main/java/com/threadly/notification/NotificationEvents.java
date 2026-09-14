package com.threadly.notification;

/**
 * Facts the rest of the domain publishes when something notification-worthy happens.
 *
 * <p>Services raise these instead of calling the notification code directly, so following,
 * liking and replying stay unaware that notifications exist at all. The listener runs after the
 * originating transaction commits: a like that is rolled back must not leave a notification
 * behind.
 */
public final class NotificationEvents {

	private NotificationEvents() {
	}

	public record Followed(Long actorId, Long recipientId) {
	}

	public record PostLiked(Long actorId, Long recipientId, Long postId) {
	}

	public record PostReplied(Long actorId, Long recipientId, Long postId) {
	}

	public record PostReposted(Long actorId, Long recipientId, Long postId, boolean quote) {
	}
}
