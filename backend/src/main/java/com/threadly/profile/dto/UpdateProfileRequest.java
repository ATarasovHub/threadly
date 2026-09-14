package com.threadly.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A partial edit: every field is optional.
 *
 * <p>Omitting a field (or sending {@code null}) leaves it as it was; sending an empty string
 * clears it. That distinction is why this is a PATCH and not a PUT.
 */
public record UpdateProfileRequest(
		@Size(min = 1, max = 50) String displayName,
		@Size(max = 160) String bio,
		@Size(max = 50) String location,

		@Size(max = 200)
		@Pattern(regexp = "|^https?://.+", message = "must be an http or https URL")
		String website,

		@Size(max = 500)
		@Pattern(regexp = "|^https?://.+", message = "must be an http or https URL")
		String avatarUrl,

		@Size(max = 500)
		@Pattern(regexp = "|^https?://.+", message = "must be an http or https URL")
		String bannerUrl) {
}
