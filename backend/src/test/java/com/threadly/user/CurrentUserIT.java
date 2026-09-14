package com.threadly.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CurrentUserIT extends ApiIntegrationTest {

	@BeforeEach
	void createAccount() {
		givenAccount("andrii");
	}

	@Test
	void returnsTheAccountBehindTheToken() throws Exception {
		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenFor("andrii")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("andrii"))
				.andExpect(jsonPath("$.displayName").value("andrii"))
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
		String token = accessTokenFor("andrii");
		refreshTokens.deleteAll();
		users.deleteAll();

		mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}
}
