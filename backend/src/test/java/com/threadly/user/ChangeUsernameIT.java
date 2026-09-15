package com.threadly.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ChangeUsernameIT extends ApiIntegrationTest {

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		token = accessTokenFor("andrii");
	}

	private org.springframework.test.web.servlet.ResultActions rename(String username) throws Exception {
		return mockMvc.perform(asUser(patch("/api/v1/me/username"), token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\"}"));
	}

	@Test
	void changesTheHandle() throws Exception {
		rename("andrii_t")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("andrii_t"));

		mockMvc.perform(asUser(get("/api/v1/users/andrii_t"), token)).andExpect(status().isOk());
	}

	@Test
	void keepsTheSessionWorkingAfterARename() throws Exception {
		rename("andrii_t").andExpect(status().isOk());

		// The access token carries the account id, not the handle, so it survives the change.
		mockMvc.perform(asUser(get("/api/v1/me"), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("andrii_t"));
	}

	@Test
	void allowsChangingOnlyTheCasingOfYourOwnHandle() throws Exception {
		rename("Andrii")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("Andrii"));
	}

	@Test
	void refusesAHandleSomeoneElseHolds() throws Exception {
		givenAccount("anna");

		rename("ANNA")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.field").value("username"));
	}

	@Test
	void rejectsAnInvalidHandle() throws Exception {
		rename("no spaces allowed")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").isNotEmpty());
	}

	@Test
	void rejectsAnonymousRenames() throws Exception {
		mockMvc.perform(patch("/api/v1/me/username")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"whatever\"}"))
				.andExpect(status().isUnauthorized());
	}
}
