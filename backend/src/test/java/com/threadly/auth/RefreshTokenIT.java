package com.threadly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.refresh.RefreshCookieFactory;
import com.threadly.auth.refresh.RefreshTokenRepository;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshTokenIT {

	private static final String PASSWORD = "sup3rsecret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void createAccount() {
		refreshTokens.deleteAll();
		users.deleteAll();
		users.save(User.builder()
				.username("andrii")
				.email("andrii@example.com")
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.displayName("Andrii")
				.role(Role.USER)
				.enabled(true)
				.build());
	}

	private Cookie login() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"andrii","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return result.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);
	}

	private Cookie refreshWith(Cookie cookie) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getCookie(RefreshCookieFactory.COOKIE_NAME);
	}

	@Test
	void loginSetsHardenedRefreshCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"andrii","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(cookie().httpOnly(RefreshCookieFactory.COOKIE_NAME, true))
				.andExpect(cookie().path(RefreshCookieFactory.COOKIE_NAME, "/api/v1/auth"))
				// The long-lived credential must never be readable from the response body.
				.andExpect(jsonPath("$.refreshToken").doesNotExist());
	}

	@Test
	void storesOnlyAHashOfTheToken() throws Exception {
		Cookie cookie = login();

		assertThat(refreshTokens.findAll()).singleElement().satisfies(stored -> {
			assertThat(stored.getTokenHash()).hasSize(64).doesNotContain(cookie.getValue());
			assertThat(stored.isRevoked()).isFalse();
		});
	}

	@Test
	void refreshReturnsANewAccessTokenAndRotatesTheCookie() throws Exception {
		Cookie first = login();
		Cookie second = refreshWith(first);

		assertThat(second.getValue()).isNotEqualTo(first.getValue());
		assertThat(refreshTokens.findAll()).hasSize(2);
	}

	@Test
	void refreshKeepsTheSessionInOneFamily() throws Exception {
		Cookie first = login();
		refreshWith(first);

		assertThat(refreshTokens.findAll())
				.extracting(token -> token.getFamilyId().toString())
				.containsOnly(refreshTokens.findAll().getFirst().getFamilyId().toString());
	}

	@Test
	void replayingARotatedTokenRevokesTheWholeFamily() throws Exception {
		Cookie first = login();
		Cookie second = refreshWith(first);

		// A stolen copy of the already-rotated token is replayed.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(first))
				.andExpect(status().isUnauthorized());

		// The legitimate holder is logged out too: with two claimants there is no way to tell
		// which one is the attacker.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(second))
				.andExpect(status().isUnauthorized());
		assertThat(refreshTokens.findAll()).allMatch(token -> token.isRevoked());
	}

	@Test
	void rejectsRefreshWithoutACookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Session expired"));
	}

	@Test
	void rejectsUnknownRefreshToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, "made-up-token")))
				.andExpect(status().isUnauthorized())
				// A failed refresh must not read as a rejected password.
				.andExpect(jsonPath("$.type").value("https://threadly.dev/problems/invalid-refresh-token"));
	}

	@Test
	void logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
		Cookie cookie = login();

		mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(RefreshCookieFactory.COOKIE_NAME, 0));

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutWithoutASessionStillSucceeds() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isNoContent());
	}
}
