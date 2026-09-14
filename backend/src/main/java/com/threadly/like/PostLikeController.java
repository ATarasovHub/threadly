package com.threadly.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts/{postId}")
@RequiredArgsConstructor
public class PostLikeController {

	private final PostLikeService postLikeService;

	@PostMapping("/like")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void like(@PathVariable Long postId) {
		postLikeService.like(postId);
	}

	@DeleteMapping("/like")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlike(@PathVariable Long postId) {
		postLikeService.unlike(postId);
	}
}
