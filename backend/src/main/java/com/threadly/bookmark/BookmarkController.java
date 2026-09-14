package com.threadly.bookmark;

import com.threadly.common.page.CursorPage;
import com.threadly.post.dto.PostResponse;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class BookmarkController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final BookmarkService bookmarkService;

	@PostMapping("/posts/{postId}/bookmark")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void save(@PathVariable Long postId) {
		bookmarkService.save(postId);
	}

	@DeleteMapping("/posts/{postId}/bookmark")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long postId) {
		bookmarkService.remove(postId);
	}

	/** Bookmarks are private, so this only ever serves the caller's own. */
	@GetMapping("/me/bookmarks")
	public CursorPage<PostResponse> saved(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return bookmarkService.saved(cursor, limit);
	}
}
