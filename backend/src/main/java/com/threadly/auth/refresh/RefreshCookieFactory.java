package com.threadly.auth.refresh;

import com.threadly.config.AuthProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the cookie carrying the refresh token.
 *
 * <p>HttpOnly keeps it out of reach of JavaScript, so an XSS bug cannot steal the long-lived
 * credential; SameSite=Strict means it is never sent on cross-site requests, which is what lets
 * the API stay CSRF-free without a token dance.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

	public static final String COOKIE_NAME = "refresh_token";

	private final AuthProperties properties;

	public ResponseCookie create(String value) {
		return base(value).maxAge(properties.refreshTokenTtl()).build();
	}

	/** A cookie that expires immediately, used to clear the browser's copy on logout. */
	public ResponseCookie expired() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
				.httpOnly(true)
				.secure(properties.cookieSecure())
				.sameSite("Strict")
				.path(properties.cookiePath());
	}
}
