package com.threadly.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param idToken the ID token issued by Google to the browser; verified server-side before it is
 *                trusted for anything
 */
public record GoogleSignInRequest(@NotBlank String idToken) {
}
