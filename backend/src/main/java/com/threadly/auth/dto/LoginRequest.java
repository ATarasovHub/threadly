package com.threadly.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param identifier handle or email address
 */
public record LoginRequest(
		@NotBlank String identifier,
		@NotBlank String password) {
}
