package com.threadly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.jwt.AccessTokenService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIT {

	private static final String PASSWORD = "sup3rsecret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void createAccount() {
		users.deleteAll();
		users.save(User.builder()
				.username("Andrii")
				.email("andrii@example.com")
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.displayName("Andrii")
				.role(Role.USER)
				.enabled(true)
				.build());
	}

	private static String credentials(String identifier, String password) {
		return """
				{"identifier":"%s","password":"%s"}
				""".formatted(identifier, password);
	}

	private String login(String identifier, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials(identifier, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceAll(".*\"accessToken\"\s*:\s*\"([^\"]+)\".*", "$1");
	}

	@Test
	void issuesBearerTokenForValidCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials("andrii", PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.user.username").value("Andrii"));
	}

	@Test
	void acceptsEmailAsIdentifier() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials("ANDRII@example.com", PASSWORD)))
				.andExpect(status().isOk());
	}

	@Test
	void signsTokenWithAccountIdRoleAndHandle() throws Exception {
		Jwt token = jwtDecoder.decode(login("andrii", PASSWORD));
		User account = users.findByUsernameIgnoreCase("andrii").orElseThrow();

		assertThat(token.getSubject()).isEqualTo(String.valueOf(account.getId()));
		assertThat(token.getClaimAsString(AccessTokenService.ROLE_CLAIM)).isEqualTo("USER");
		assertThat(token.getClaimAsString(AccessTokenService.USERNAME_CLAIM)).isEqualTo("Andrii");
		assertThat(token.getIssuer()).hasToString("https://threadly.dev");
		assertThat(token.getExpiresAt()).isNotNull();
	}

	@Test
	void rejectsWrongPasswordWithoutRevealingWhetherAccountExists() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials("andrii", "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Invalid credentials"))
				.andExpect(jsonPath("$.detail").value("The identifier or password is incorrect."));
	}

	@Test
	void rejectsUnknownAccountWithTheSameResponseAsAWrongPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials("ghost", PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("The identifier or password is incorrect."));
	}

	@Test
	void rejectsDisabledAccount() throws Exception {
		User account = users.findByUsernameIgnoreCase("andrii").orElseThrow();
		account.setEnabled(false);
		users.save(account);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(credentials("andrii", PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void acceptsIssuedTokenOnProtectedEndpoints() throws Exception {
		String token = login("andrii", PASSWORD);

		// The endpoint does not exist yet, so a 404 proves the token passed authentication;
		// without it the same call is answered with 401.
		mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsTamperedToken() throws Exception {
		String token = login("andrii", PASSWORD);
		String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "not-a-valid-signature";

		mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}
}
