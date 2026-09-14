package com.threadly.post;

import com.threadly.common.page.CursorPage;
import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.dto.UpdatePostRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class PostController {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final PostService postService;

	@PostMapping("/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(@Valid @RequestBody CreatePostRequest request) {
		return postService.create(request);
	}

	@PostMapping("/posts/{id}/replies")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse reply(@PathVariable Long id, @Valid @RequestBody CreatePostRequest request) {
		return postService.reply(id, request);
	}

	/** Replies are returned oldest first, so a thread reads top to bottom. */
	@GetMapping("/posts/{id}/replies")
	public CursorPage<PostResponse> replies(
			@PathVariable Long id,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return postService.repliesTo(id, cursor, limit);
	}

	@PostMapping("/posts/{id}/repost")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void repost(@PathVariable Long id) {
		postService.repost(id);
	}

	@DeleteMapping("/posts/{id}/repost")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void undoRepost(@PathVariable Long id) {
		postService.undoRepost(id);
	}

	@PostMapping("/posts/{id}/quote")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse quote(@PathVariable Long id, @Valid @RequestBody CreatePostRequest request) {
		return postService.quote(id, request);
	}

	@GetMapping("/posts/{id}")
	public PostResponse findById(@PathVariable Long id) {
		return postService.findById(id);
	}

	@PatchMapping("/posts/{id}")
	public PostResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
		return postService.update(id, request);
	}

	@DeleteMapping("/posts/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		postService.delete(id);
	}

	/**
	 * @param cursor opaque token from the previous page; omit it for the first page
	 * @param limit  capped so one request cannot ask for an unbounded page
	 */
	@GetMapping("/users/{username}/posts")
	public CursorPage<PostResponse> timeline(
			@PathVariable String username,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int limit) {
		return postService.timelineOf(username, cursor, limit);
	}
}
