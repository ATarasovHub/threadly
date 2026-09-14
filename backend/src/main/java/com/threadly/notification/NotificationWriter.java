package com.threadly.notification;

import com.threadly.post.Post;
import com.threadly.post.PostRepository;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Records notifications in response to domain events.
 *
 * <p>Listens after commit, so an action that is rolled back leaves no notification behind, and in
 * its own transaction, since the originating one is already finished by then. A failure here is
 * logged rather than rethrown: missing a notification must not undo the like or follow that
 * caused it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWriter {

	private final NotificationRepository notifications;
	private final UserRepository users;
	private final PostRepository posts;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onFollowed(NotificationEvents.Followed event) {
		record(event.actorId(), event.recipientId(), NotificationType.FOLLOW, null);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPostLiked(NotificationEvents.PostLiked event) {
		record(event.actorId(), event.recipientId(), NotificationType.LIKE, event.postId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPostReplied(NotificationEvents.PostReplied event) {
		record(event.actorId(), event.recipientId(), NotificationType.REPLY, event.postId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPostReposted(NotificationEvents.PostReposted event) {
		NotificationType type = event.quote() ? NotificationType.QUOTE : NotificationType.REPOST;
		record(event.actorId(), event.recipientId(), type, event.postId());
	}

	private void record(Long actorId, Long recipientId, NotificationType type, Long postId) {
		// Acting on your own content is not news.
		if (actorId.equals(recipientId)) {
			return;
		}

		try {
			User actor = users.findById(actorId).orElse(null);
			User recipient = users.findById(recipientId).orElse(null);
			if (actor == null || recipient == null) {
				return;
			}
			Post post = postId == null ? null : posts.findById(postId).orElse(null);

			notifications.save(Notification.of(recipient, actor, type, post));
		}
		catch (RuntimeException e) {
			log.error("Failed to record {} notification for account {}", type, recipientId, e);
		}
	}
}
