package com.threadly.notification;

import com.threadly.common.page.CursorPage;
import com.threadly.notification.dto.NotificationResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final NotificationService notificationService;

	@GetMapping
	public CursorPage<NotificationResponse> inbox(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return notificationService.inbox(cursor, limit);
	}

	@GetMapping("/unread-count")
	public Map<String, Long> unreadCount() {
		return Map.of("unread", notificationService.unreadCount());
	}

	@PostMapping("/read")
	public Map<String, Integer> markAllRead() {
		return Map.of("marked", notificationService.markAllRead());
	}
}
