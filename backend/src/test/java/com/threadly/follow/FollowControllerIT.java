package com.threadly.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FollowControllerIT extends ApiIntegrationTest {

	private static final Pattern USERNAME = Pattern.compile("\"username\":\"([^\"]+)\"");
	private static final Pattern NEXT_CURSOR = Pattern.compile("\"nextCursor\":\"([^\"]+)\"");

	@Autowired
	private FollowRepository follows;

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		token = accessTokenFor("andrii");
	}

	private void follow(String username, String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/" + username + "/follow"), asToken))
				.andExpect(status().isNoContent());
	}

	private static List<String> usernamesOf(String body) {
		List<String> found = new ArrayList<>();
		Matcher matcher = USERNAME.matcher(body);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return found;
	}

	private static String nextCursorOf(String body) {
		Matcher matcher = NEXT_CURSOR.matcher(body);
		return matcher.find() ? matcher.group(1) : null;
	}

	private String listOf(String path, String cursor, int limit) throws Exception {
		var request = get(path).param("limit", String.valueOf(limit));
		if (cursor != null) {
			request = request.param("cursor", cursor);
		}
		return mockMvc.perform(asUser(request, token))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	@Test
	void createsAnEdgeWhenFollowing() throws Exception {
		givenAccount("anna");

		follow("anna", token);

		assertThat(follows.count()).isEqualTo(1);
		assertThat(usernamesOf(listOf("/api/v1/users/anna/followers", null, 20)))
				.containsExactly("andrii");
		assertThat(usernamesOf(listOf("/api/v1/users/andrii/following", null, 20)))
				.containsExactly("anna");
	}

	@Test
	void followingTwiceLeavesOneEdge() throws Exception {
		givenAccount("anna");

		follow("anna", token);
		// Following is a state, not an event: repeating the request must not duplicate the edge
		// or fail, so a client can retry safely.
		follow("anna", token);

		assertThat(follows.count()).isEqualTo(1);
	}

	@Test
	void unfollowRemovesTheEdgeAndIsIdempotent() throws Exception {
		givenAccount("anna");
		follow("anna", token);

		mockMvc.perform(asUser(delete("/api/v1/users/anna/follow"), token))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(delete("/api/v1/users/anna/follow"), token))
				.andExpect(status().isNoContent());

		assertThat(follows.count()).isZero();
	}

	@Test
	void refusesSelfFollow() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("You cannot follow yourself."));

		assertThat(follows.count()).isZero();
	}

	@Test
	void followIsDirected() throws Exception {
		givenAccount("anna");
		follow("anna", token);

		// Andrii following Anna says nothing about Anna following Andrii.
		assertThat(usernamesOf(listOf("/api/v1/users/andrii/followers", null, 20))).isEmpty();
		assertThat(usernamesOf(listOf("/api/v1/users/anna/following", null, 20))).isEmpty();
	}

	@Test
	void answersUnknownHandleWithNotFound() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/ghost/follow"), token))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnonymousFollow() throws Exception {
		givenAccount("anna");

		mockMvc.perform(post("/api/v1/users/anna/follow"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listsFollowersNewestFirstAcrossPages() throws Exception {
		List<String> handles = List.of("bob", "carol", "dave", "erin", "frank");
		for (String handle : handles) {
			givenAccount(handle);
			follow("andrii", accessTokenFor(handle));
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			String body = listOf("/api/v1/users/andrii/followers", cursor, 2);
			seen.addAll(usernamesOf(body));
			cursor = nextCursorOf(body);
		}
		while (cursor != null);

		assertThat(seen).containsExactly("frank", "erin", "dave", "carol", "bob");
	}

	@Test
	void dropsFollowEdgesWhenAnAccountIsDeleted() throws Exception {
		givenAccount("anna");
		follow("anna", token);

		users.delete(users.findByUsernameIgnoreCase("anna").orElseThrow());

		// ON DELETE CASCADE keeps the graph from pointing at accounts that no longer exist.
		assertThat(follows.count()).isZero();
	}
}
