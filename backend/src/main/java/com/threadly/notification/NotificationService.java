package com.threadly.notification;

import com.threadly.common.page.Cursor;
import com.threadly.common.page.CursorPage;
import com.threadly.common.page.CursorPaging;
import com.threadly.notification.dto.NotificationResponse;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notifications;
	private final CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public CursorPage<NotificationResponse> inbox(String encodedCursor, int limit) {
		User me = currentUserService.require();
		return CursorPaging.page(
				encodedCursor,
				limit,
				window -> notifications.findInbox(me.getId(), window),
				(position, window) -> notifications.findInboxBefore(
						me.getId(), position.createdAt(), position.id(), window),
				notification -> new Cursor(notification.getCreatedAt(), notification.getId()),
				NotificationResponse::from);
	}

	@Transactional(readOnly = true)
	public long unreadCount() {
		return notifications.countByRecipientIdAndReadAtIsNull(currentUserService.require().getId());
	}

	/** Marks the whole inbox read. Returns how many rows changed, so the client can update a badge. */
	@Transactional
	public int markAllRead() {
		return notifications.markAllRead(currentUserService.require().getId(), Instant.now());
	}
}
