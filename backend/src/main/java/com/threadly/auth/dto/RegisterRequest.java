package com.threadly.auth.dto;

import com.threadly.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param password raw password; strength rules and the 72-byte BCrypt ceiling live in
 *                 {@link StrongPassword}
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
		@StrongPassword
		String password,

		@NotBlank
		@Size(max = 50)
		String displayName) {
}
