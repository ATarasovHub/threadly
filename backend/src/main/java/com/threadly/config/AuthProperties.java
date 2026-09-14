package com.threadly.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param refreshTokenTtl how long a refresh token, and therefore a session, can live
 * @param cookieSecure    whether the refresh cookie is marked Secure; false only for plain-HTTP
 *                        local development
 * @param cookiePath      scope of the refresh cookie, so it is not attached to every API call
 */
@ConfigurationProperties(prefix = "threadly.auth")
public record AuthProperties(Duration refreshTokenTtl, boolean cookieSecure, String cookiePath) {

	public AuthProperties {
		refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(30) : refreshTokenTtl;
		cookiePath = cookiePath == null ? "/api/v1/auth" : cookiePath;
	}
}
