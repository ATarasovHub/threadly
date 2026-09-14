package com.threadly.follow;

import com.threadly.common.page.CursorPage;
import com.threadly.follow.dto.UserSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{username}")
@RequiredArgsConstructor
@Validated
public class FollowController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final FollowService followService;

	@PostMapping("/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void follow(@PathVariable String username) {
		followService.follow(username);
	}

	@DeleteMapping("/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollow(@PathVariable String username) {
		followService.unfollow(username);
	}

	@GetMapping("/followers")
	public CursorPage<UserSummaryResponse> followers(
			@PathVariable String username,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return followService.followersOf(username, cursor, limit);
	}

	@GetMapping("/following")
	public CursorPage<UserSummaryResponse> following(
			@PathVariable String username,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return followService.followingOf(username, cursor, limit);
	}
}
