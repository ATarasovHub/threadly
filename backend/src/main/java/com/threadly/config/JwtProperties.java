package com.threadly.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param secret          HMAC key for HS256; must be at least 32 characters so the derived key
 *                        reaches the 256 bits the algorithm requires
 * @param issuer          value of the {@code iss} claim; must be a URI, as the spec requires
 * @param accessTokenTtl  how long an access token stays valid
 */
@ConfigurationProperties(prefix = "threadly.jwt")
@Validated
public record JwtProperties(
		@NotBlank @Size(min = 32) String secret,
		@NotBlank String issuer,
		Duration accessTokenTtl) {

	public JwtProperties {
		accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
	}
}
