package com.threadly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearAccounts() {
		users.deleteAll();
	}

	private static String body(String username, String email, String password, String displayName) {
		return """
				{"username":"%s","email":"%s","password":"%s","displayName":"%s"}
				""".formatted(username, email, password, displayName);
	}

	@Test
	void registersAccountAndReturnsPublicProfile() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("andrii", "andrii@example.com", "sup3rsecret", "Andrii")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value("andrii"))
				.andExpect(jsonPath("$.displayName").value("Andrii"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				// The response must never leak the email or anything credential-shaped.
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void storesPasswordAsBcryptHash() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("andrii", "andrii@example.com", "sup3rsecret", "Andrii")))
				.andExpect(status().isCreated());

		User stored = users.findByUsernameIgnoreCase("andrii").orElseThrow();
		assertThat(stored.getPasswordHash()).startsWith("$2a$12$");
		assertThat(passwordEncoder.matches("sup3rsecret", stored.getPasswordHash())).isTrue();
	}

	@Test
	void rejectsHandleAlreadyTakenRegardlessOfCase() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("andrii", "andrii@example.com", "sup3rsecret", "Andrii")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("ANDRII", "other@example.com", "sup3rsecret", "Other")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.field").value("username"));
	}

	@Test
	void rejectsEmailAlreadyRegistered() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("andrii", "andrii@example.com", "sup3rsecret", "Andrii")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("anna", "ANDRII@example.com", "sup3rsecret", "Anna")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.field").value("email"));
	}

	@Test
	void reportsEveryInvalidFieldAtOnce() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("a b", "not-an-email", "short", "Andrii")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.errors.username").isNotEmpty())
				.andExpect(jsonPath("$.errors.email").isNotEmpty())
				.andExpect(jsonPath("$.errors.password").isNotEmpty());
	}

	@Test
	void answersInEnglishEvenWhenTheClientAsksForAnotherLanguage() throws Exception {
		// The locale is pinned, so default Bean Validation messages cannot come back translated.
		mockMvc.perform(post("/api/v1/auth/register")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU,ru;q=0.9")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("andrii", "not-an-email", "sup3rsecret", "Andrii")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));
	}

	@Test
	void requiresAuthenticationForEverythingElse() throws Exception {
		mockMvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}
}
