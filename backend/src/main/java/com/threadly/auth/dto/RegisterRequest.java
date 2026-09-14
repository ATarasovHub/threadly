package com.threadly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param password raw password; BCrypt only considers the first 72 bytes, hence the upper bound.
 */
public record RegisterRequest(
		@NotBlank
		@Pattern(regexp = "^[A-Za-z0-9_]{3,30}$",
				message = "must be 3-30 characters of letters, digits or underscores")
		String username,

		@NotBlank
		@Email
		@Size(max = 254)
		String email,

		@NotBlank
		@Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
		String password,

		@NotBlank
		@Size(max = 50)
		String displayName) {
}
