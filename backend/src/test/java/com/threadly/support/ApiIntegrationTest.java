package com.threadly.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.refresh.RefreshTokenRepository;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Shared setup for tests that drive the API end to end against a PostgreSQL container.
 *
 * <p>Every subclass gets an empty database and the helpers needed to create an account and act as
 * it. The Spring context is cached across subclasses because the annotations here are identical,
 * so the container starts once for the whole suite.
 */
// Rate limiting is exercised by its own test. Leaving it on here would make unrelated tests
// share one address-keyed quota and fail in bursts.
@SpringBootTest(properties = "threadly.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiIntegrationTest {

	protected static final String PASSWORD = "sup3rsecret";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UserRepository users;

	@Autowired
	protected RefreshTokenRepository refreshTokens;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	/** Runs before each subclass's own {@code @BeforeEach}, so every test starts from nothing. */
	@BeforeEach
	void resetDatabase() {
		refreshTokens.deleteAll();
		users.deleteAll();
	}

	protected User givenAccount(String username) {
		return givenAccount(username, true);
	}

	protected User givenAccount(String username, boolean enabled) {
		return users.save(User.builder()
				.username(username)
				.email(username.toLowerCase() + "@example.com")
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.displayName(username)
				.role(Role.USER)
				.enabled(enabled)
				.build());
	}

	/** Logs in through the real endpoint, so the token is minted exactly as a client's would be. */
	protected String accessTokenFor(String identifier) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"%s","password":"%s"}
								""".formatted(identifier, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceAll(".*\"accessToken\"\s*:\s*\"([^\"]+)\".*", "$1");
	}

	protected MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, String token) {
		return request.header("Authorization", "Bearer " + token);
	}
}
