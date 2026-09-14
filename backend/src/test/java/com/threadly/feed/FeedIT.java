package com.threadly.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FeedIT extends ApiIntegrationTest {

	private static final Pattern CONTENT = Pattern.compile("\"content\":\"([^\"]+)\"");
	private static final Pattern NEXT_CURSOR = Pattern.compile("\"nextCursor\":\"([^\"]+)\"");

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		token = accessTokenFor("andrii");
	}

	private void writePost(String handle, String content) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts"), accessTokenFor(handle))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated());
	}

	private void follow(String handle) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/" + handle + "/follow"), token))
				.andExpect(status().isNoContent());
	}

	private String feed(String which, String cursor, int limit) throws Exception {
		var request = get("/api/v1/feed/" + which).param("limit", String.valueOf(limit));
		if (cursor != null) {
			request = request.param("cursor", cursor);
		}
		return mockMvc.perform(asUser(request, token))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private static List<String> contentsOf(String body) {
		List<String> found = new ArrayList<>();
		Matcher matcher = CONTENT.matcher(body);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return found;
	}

	private static String nextCursorOf(String body) {
		Matcher matcher = NEXT_CURSOR.matcher(body);
		return matcher.find() ? matcher.group(1) : null;
	}

	@Test
	void followingFeedShowsOnlyFollowedAccountsAndYourself() throws Exception {
		givenAccount("anna");
		givenAccount("bob");
		follow("anna");

		writePost("anna", "from anna");
		writePost("bob", "from bob");
		writePost("andrii", "from me");

		assertThat(contentsOf(feed("following", null, 20)))
				.containsExactly("from me", "from anna");
	}

	@Test
	void followingFeedIsEmptyWithoutFollowsOrPosts() throws Exception {
		givenAccount("anna");
		writePost("anna", "from anna");

		assertThat(contentsOf(feed("following", null, 20))).isEmpty();
	}

	@Test
	void unfollowingRemovesPostsFromTheFollowingFeed() throws Exception {
		givenAccount("anna");
		follow("anna");
		writePost("anna", "from anna");
		assertThat(contentsOf(feed("following", null, 20))).containsExactly("from anna");

		mockMvc.perform(asUser(delete("/api/v1/users/anna/follow"), token))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(feed("following", null, 20))).isEmpty();
	}

	@Test
	void forYouFeedShowsEveryoneNewestFirst() throws Exception {
		givenAccount("anna");
		givenAccount("bob");
		writePost("anna", "first");
		writePost("bob", "second");
		writePost("andrii", "third");

		assertThat(contentsOf(feed("for-you", null, 20)))
				.containsExactly("third", "second", "first");
	}

	@Test
	void walksTheFollowingFeedExactlyOnceAcrossPages() throws Exception {
		givenAccount("anna");
		follow("anna");
		for (int i = 1; i <= 5; i++) {
			writePost("anna", "post-" + i);
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			String body = feed("following", cursor, 2);
			seen.addAll(contentsOf(body));
			cursor = nextCursorOf(body);
		}
		while (cursor != null);

		assertThat(seen).containsExactly("post-5", "post-4", "post-3", "post-2", "post-1");
	}

	@Test
	void leavesDeletedPostsOutOfBothFeeds() throws Exception {
		givenAccount("anna");
		follow("anna");
		writePost("anna", "keep");
		writePost("anna", "remove");

		String body = feed("for-you", null, 20);
		Matcher matcher = Pattern.compile("[{,]\"id\":(\\d+),\"content\":\"remove\"").matcher(body);
		assertThat(matcher.find()).isTrue();
		mockMvc.perform(asUser(delete("/api/v1/posts/" + matcher.group(1)), accessTokenFor("anna")))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(feed("following", null, 20))).containsExactly("keep");
		assertThat(contentsOf(feed("for-you", null, 20))).containsExactly("keep");
	}

	@Test
	void rejectsAnonymousFeedRequests() throws Exception {
		mockMvc.perform(get("/api/v1/feed/following")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/feed/for-you")).andExpect(status().isUnauthorized());
	}
}
