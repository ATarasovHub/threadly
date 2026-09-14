package com.threadly.feed;

import com.threadly.common.page.CursorPage;
import com.threadly.post.dto.PostResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Validated
public class FeedController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final FeedService feedService;

	@GetMapping("/following")
	public CursorPage<PostResponse> following(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return feedService.following(cursor, limit);
	}

	@GetMapping("/for-you")
	public CursorPage<PostResponse> forYou(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return feedService.forYou(cursor, limit);
	}
}
