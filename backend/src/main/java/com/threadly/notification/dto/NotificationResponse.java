package com.threadly.notification.dto;

import com.threadly.notification.Notification;
import com.threadly.notification.NotificationType;
import java.time.Instant;

/**
 * @param post a short excerpt, enough to render the row without loading the post itself
 */
public record NotificationResponse(
		Long id,
		NotificationType type,
		Actor actor,
		PostExcerpt post,
		boolean read,
		Instant createdAt) {

	private static final int EXCERPT_LENGTH = 80;

	public static NotificationResponse from(Notification notification) {
		var actor = notification.getActor();
		PostExcerpt excerpt = notification.getPost() == null
				? null
				: new PostExcerpt(notification.getPost().getId(), excerptOf(notification.getPost().getContent()));

		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				new Actor(actor.getId(), actor.getUsername(), actor.getDisplayName(), actor.getAvatarUrl()),
				excerpt,
				notification.isRead(),
				notification.getCreatedAt());
	}

	private static String excerptOf(String content) {
		if (content == null) {
			return null;
		}
		return content.length() <= EXCERPT_LENGTH ? content : content.substring(0, EXCERPT_LENGTH) + "…";
	}

	public record Actor(Long id, String username, String displayName, String avatarUrl) {
	}

	public record PostExcerpt(Long id, String excerpt) {
	}
}
