package com.threadly.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import com.threadly.auth.refresh.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfileControllerIT {

	private static final String PASSWORD = "sup3rsecret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String token;

	@BeforeEach
	void createAccountAndSignIn() throws Exception {
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

		String response = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"andrii","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		token = response.replaceAll(".*\"accessToken\"\s*:\s*\"([^\"]+)\".*", "$1");
	}

	private MockHttpServletRequestBuilder patchProfile(String body) {
		return patch("/api/v1/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	@Test
	void fillsInAnEmptyProfile() throws Exception {
		mockMvc.perform(patchProfile("""
						{"displayName":"Andrii Tarasov","bio":"Backend engineer","location":"Berlin",
						 "website":"https://tarasov.dev","avatarUrl":"https://cdn.example.com/a.png"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Andrii Tarasov"))
				.andExpect(jsonPath("$.bio").value("Backend engineer"))
				.andExpect(jsonPath("$.location").value("Berlin"))
				.andExpect(jsonPath("$.website").value("https://tarasov.dev"))
				.andExpect(jsonPath("$.avatarUrl").value("https://cdn.example.com/a.png"))
				.andExpect(jsonPath("$.username").value("andrii"))
				.andExpect(jsonPath("$.joinedAt").isNotEmpty());
	}

	@Test
	void leavesOmittedFieldsUntouched() throws Exception {
		mockMvc.perform(patchProfile("""
						{"bio":"Backend engineer","location":"Berlin"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(patchProfile("""
						{"location":"Hamburg"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.location").value("Hamburg"))
				// Untouched because it was not part of the request.
				.andExpect(jsonPath("$.bio").value("Backend engineer"));
	}

	@Test
	void clearsAFieldSentAsAnEmptyString() throws Exception {
		mockMvc.perform(patchProfile("""
						{"bio":"Backend engineer"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(patchProfile("""
						{"bio":""}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").doesNotExist());
	}

	@Test
	void trimsSurroundingWhitespace() throws Exception {
		mockMvc.perform(patchProfile("""
						{"location":"  Berlin  "}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.location").value("Berlin"));
	}

	@Test
	void treatsWhitespaceOnlyAsClearing() throws Exception {
		mockMvc.perform(patchProfile("""
						{"bio":"Backend engineer"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(patchProfile("""
						{"bio":"   "}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").doesNotExist());
	}

	@Test
	void rejectsWebsiteWithoutAnHttpScheme() throws Exception {
		mockMvc.perform(patchProfile("""
						{"website":"javascript:alert(1)"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.website").value("must be an http or https URL"));
	}

	@Test
	void rejectsBioLongerThanTheLimit() throws Exception {
		mockMvc.perform(patchProfile("""
						{"bio":"%s"}
						""".formatted("x".repeat(161))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.bio").isNotEmpty());
	}

	@Test
	void servesAnotherAccountsProfileByHandle() throws Exception {
		users.save(User.builder()
				.username("Anna")
				.email("anna@example.com")
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.displayName("Anna")
				.role(Role.USER)
				.enabled(true)
				.build());

		// The handle in the URL is matched case-insensitively, so /anna and /Anna are one page.
		mockMvc.perform(get("/api/v1/users/anna").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("Anna"))
				.andExpect(jsonPath("$.joinedAt").isNotEmpty())
				// A profile is public information; credentials and contact details are not.
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.role").doesNotExist());
	}

	@Test
	void showsProfileEditsOnThePublicPage() throws Exception {
		mockMvc.perform(patchProfile("""
						{"bio":"Backend engineer","location":"Berlin"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/users/ANDRII").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bio").value("Backend engineer"))
				.andExpect(jsonPath("$.location").value("Berlin"));
	}

	@Test
	void answersUnknownHandleWithNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/users/ghost").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Not found"))
				.andExpect(jsonPath("$.type").value("https://threadly.dev/problems/not-found"));
	}

	@Test
	void hidesDisabledAccounts() throws Exception {
		User disabled = users.save(User.builder()
				.username("banned")
				.email("banned@example.com")
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.displayName("Banned")
				.role(Role.USER)
				.enabled(false)
				.build());

		mockMvc.perform(get("/api/v1/users/" + disabled.getUsername())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnonymousProfileLookup() throws Exception {
		mockMvc.perform(get("/api/v1/users/andrii"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnonymousEdit() throws Exception {
		mockMvc.perform(patch("/api/v1/me/profile")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"bio":"anything"}
								"""))
				.andExpect(status().isUnauthorized());
	}
}
