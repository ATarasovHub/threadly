package com.threadly.auth;

import com.threadly.auth.dto.AuthenticationResponse;
import com.threadly.auth.dto.GoogleSignInRequest;
import com.threadly.auth.dto.LoginRequest;
import com.threadly.auth.dto.RegisterRequest;
import com.threadly.auth.refresh.InvalidRefreshTokenException;
import com.threadly.auth.refresh.RefreshCookieFactory;
import com.threadly.identity.google.GoogleAuthService;
import com.threadly.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshCookieFactory refreshCookieFactory;
	private final GoogleAuthService googleAuthService;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
		return withRefreshCookie(authService.login(request));
	}

	/** Exchanges a Google ID token for a Threadly session. */
	@PostMapping("/google")
	public ResponseEntity<AuthenticationResponse> signInWithGoogle(
			@Valid @RequestBody GoogleSignInRequest request) {
		return withRefreshCookie(googleAuthService.signIn(request.idToken()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthenticationResponse> refresh(
			@CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException("Missing refresh token");
		}
		return withRefreshCookie(authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ResponseEntity<Void> logout(
			@CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			authService.logout(refreshToken);
		}
		// Clear the cookie even when no session was found, so a stale copy cannot linger.
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expired().toString())
				.build();
	}

	private ResponseEntity<AuthenticationResponse> withRefreshCookie(AuthService.AuthenticatedSession session) {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookieFactory.create(session.refreshToken()).toString())
				.body(session.body());
	}
}
