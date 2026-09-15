package com.threadly.identity.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.refresh.RefreshCookieFactory;
import com.threadly.auth.refresh.RefreshTokenRepository;
import com.threadly.identity.IdentityProvider;
import com.threadly.identity.UserIdentityRepository;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"threadly.rate-limit.enabled=false",
		"threadly.oauth.google.client-id=" + GoogleTokens.CLIENT_ID
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, GoogleTestDecoderConfiguration.class})
class GoogleSignInIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private UserIdentityRepository identities;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void reset() {
		refreshTokens.deleteAll();
		identities.deleteAll();
		users.deleteAll();
	}

	private org.springframework.test.web.servlet.ResultActions signIn(String idToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/google")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"idToken\":\"" + idToken + "\"}"));
	}

	@Test
	void createsAnAccountOnFirstSignIn() throws Exception {
		String token = GoogleTokens.idToken("google-subject-1")
				.email("andrii.tarasov@example.com")
				.name("Andrii Tarasov")
				.sign();

		signIn(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.user.displayName").value("Andrii Tarasov"))
				// The refresh cookie is issued exactly as it is for a password sign-in.
				.andExpect(cookie().httpOnly(RefreshCookieFactory.COOKIE_NAME, true));

		User created = users.findByEmailIgnoreCase("andrii.tarasov@example.com").orElseThrow();
		// A handle is derived from the address; Google never supplies one.
		assertThat(created.getUsername()).startsWith("andriitarasov");
		assertThat(created.hasPassword()).isFalse();
	}

	@Test
	void reusesTheSameAccountOnEverySubsequentSignIn() throws Exception {
		String token = GoogleTokens.idToken("google-subject-1").email("a@example.com").sign();

		signIn(token).andExpect(status().isOk());
		signIn(GoogleTokens.idToken("google-subject-1").email("a@example.com").sign())
				.andExpect(status().isOk());

		assertThat(users.count()).isEqualTo(1);
		assertThat(identities.count()).isEqualTo(1);
	}

	@Test
	void linksGoogleToAnExistingAccountWithTheSameVerifiedAddress() throws Exception {
		users.save(User.builder()
				.username("andrii")
				.email("andrii@example.com")
				.passwordHash(passwordEncoder.encode("Thread_ly2026"))
				.displayName("Andrii")
				.role(Role.USER)
				.enabled(true)
				.build());

		signIn(GoogleTokens.idToken("google-subject-1").email("andrii@example.com").sign())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value("andrii"));

		// One account, now reachable both ways.
		assertThat(users.count()).isEqualTo(1);
		User linked = users.findByUsernameIgnoreCase("andrii").orElseThrow();
		assertThat(identities.existsByUserIdAndProvider(linked.getId(), IdentityProvider.GOOGLE)).isTrue();
		assertThat(linked.hasPassword()).isTrue();
	}

	@Test
	void refusesToLinkAnUnverifiedAddress() throws Exception {
		users.save(User.builder()
				.username("andrii")
				.email("andrii@example.com")
				.passwordHash(passwordEncoder.encode("Thread_ly2026"))
				.displayName("Andrii")
				.role(Role.USER)
				.enabled(true)
				.build());

		// Without this rule, anyone who could register that address at Google would take over
		// the Threadly account it belongs to.
		signIn(GoogleTokens.idToken("attacker")
						.email("andrii@example.com")
						.emailVerified(false)
						.sign())
				.andExpect(status().isUnauthorized());

		assertThat(identities.count()).isZero();
	}

	@Test
	void rejectsATokenMintedForAnotherApplication() throws Exception {
		signIn(GoogleTokens.idToken("google-subject-1")
						.audience("some-other-app.apps.googleusercontent.com")
						.sign())
				.andExpect(status().isUnauthorized());

		assertThat(users.count()).isZero();
	}

	@Test
	void rejectsATokenFromAnUnexpectedIssuer() throws Exception {
		signIn(GoogleTokens.idToken("google-subject-1").issuer("https://evil.example.com").sign())
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnExpiredToken() throws Exception {
		signIn(GoogleTokens.idToken("google-subject-1").expiredAlready().sign())
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsGarbage() throws Exception {
		signIn("not-a-jwt").andExpect(status().isUnauthorized());
	}

	@Test
	void refusesPasswordSignInForAGoogleOnlyAccount() throws Exception {
		signIn(GoogleTokens.idToken("google-subject-1").email("a@example.com").sign())
				.andExpect(status().isOk());
		String handle = users.findByEmailIgnoreCase("a@example.com").orElseThrow().getUsername();

		// There is no password to compare against; the answer must be a plain 401, not a 500.
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"identifier\":\"" + handle + "\",\"password\":\"anything-at-all\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Invalid credentials"));
	}

	@Test
	void givesTheNewAccountAWorkingSession() throws Exception {
		String body = signIn(GoogleTokens.idToken("google-subject-1").email("a@example.com").sign())
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String accessToken = body.replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("a@example.com"));
	}

	@Test
	void derivesADistinctHandleWhenTheObviousOneIsTaken() throws Exception {
		users.save(User.builder()
				.username("andrii")
				.email("someone.else@example.com")
				.passwordHash(passwordEncoder.encode("Thread_ly2026"))
				.displayName("Someone")
				.role(Role.USER)
				.enabled(true)
				.build());

		signIn(GoogleTokens.idToken("google-subject-1").email("andrii@example.com").sign())
				.andExpect(status().isOk());

		String handle = users.findByEmailIgnoreCase("andrii@example.com").orElseThrow().getUsername();
		assertThat(handle).startsWith("andrii").isNotEqualTo("andrii");
	}
}
