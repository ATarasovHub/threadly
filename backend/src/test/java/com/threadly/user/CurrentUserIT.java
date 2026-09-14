package com.threadly.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.refresh.RefreshTokenRepository;
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
class CurrentUserIT {

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

	private String accessToken() throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"andrii","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceAll(".*\"accessToken\"\s*:\s*\"([^\"]+)\".*", "$1");
	}

	@Test
	void returnsTheAccountBehindTheToken() throws Exception {
		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("andrii"))
				.andExpect(jsonPath("$.displayName").value("Andrii"))
				// Unlike the public profile, an account may see its own email and role.
				.andExpect(jsonPath("$.email").value("andrii@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void rejectsRequestWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.title").value("Authentication required"));
	}

	@Test
	void rejectsTokenWhoseAccountIsGone() throws Exception {
		String token = accessToken();
		refreshTokens.deleteAll();
		users.deleteAll();

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}
}
