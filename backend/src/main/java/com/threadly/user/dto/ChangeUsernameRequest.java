package com.threadly.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUsernameRequest(
		@NotBlank
		@Pattern(regexp = "^[A-Za-z0-9_]{3,30}$",
				message = "must be 3-30 characters of letters, digits or underscores")
		String username) {
}
