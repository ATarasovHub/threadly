package com.threadly.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
		@NotBlank
		@Size(max = 500, message = "must be at most 500 characters")
		String content) {
}
