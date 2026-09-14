package com.threadly.bookmark;

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
import org.springframework.http.MediaType;

class BookmarkIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");
	private static final Pattern CONTENT = Pattern.compile("\"content\":\"([^\"]+)\"");

	private String mine;
	private String hers;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
	}

	private long writePost(String asToken, String content) throws Exception {
		String body = mockMvc.perform(asUser(post("/api/v1/posts"), asToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Matcher matcher = POST_ID.matcher(body);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}

	private void bookmark(long postId, String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/bookmark"), asToken))
				.andExpect(status().isNoContent());
	}

	private static List<String> contentsOf(String body) {
		List<String> found = new ArrayList<>();
		Matcher matcher = CONTENT.matcher(body);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return found;
	}

	private String savedList(String asToken) throws Exception {
		return mockMvc.perform(asUser(get("/api/v1/me/bookmarks"), asToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	@Test
	void savesAPostAndListsIt() throws Exception {
		long id = writePost(hers, "worth keeping");

		bookmark(id, mine);

		assertThat(contentsOf(savedList(mine))).containsExactly("worth keeping");
	}

	@Test
	void keepsBookmarksPrivate() throws Exception {
		long id = writePost(hers, "worth keeping");
		bookmark(id, mine);

		// Anna wrote the post but did not save it; nothing tells her that Andrii did.
		assertThat(contentsOf(savedList(hers))).isEmpty();
		mockMvc.perform(asUser(get("/api/v1/posts/" + id), hers))
				.andExpect(jsonPath("$.viewer.bookmarked").value(false));
		mockMvc.perform(asUser(get("/api/v1/posts/" + id), mine))
				.andExpect(jsonPath("$.viewer.bookmarked").value(true));
	}

	@Test
	void savingTwiceKeepsOneEntry() throws Exception {
		long id = writePost(hers, "worth keeping");

		bookmark(id, mine);
		bookmark(id, mine);

		assertThat(contentsOf(savedList(mine))).containsExactly("worth keeping");
	}

	@Test
	void removingIsIdempotent() throws Exception {
		long id = writePost(hers, "worth keeping");
		bookmark(id, mine);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + id + "/bookmark"), mine))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(delete("/api/v1/posts/" + id + "/bookmark"), mine))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(savedList(mine))).isEmpty();
	}

	@Test
	void ordersByWhenSavedNotWhenWritten() throws Exception {
		long older = writePost(hers, "written first");
		long newer = writePost(hers, "written second");

		// Saved in the opposite order to how they were written.
		bookmark(newer, mine);
		bookmark(older, mine);

		assertThat(contentsOf(savedList(mine)))
				.containsExactly("written first", "written second");
	}

	@Test
	void dropsDeletedPostsFromTheSavedList() throws Exception {
		long id = writePost(hers, "temporary");
		bookmark(id, mine);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + id), hers))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(savedList(mine))).isEmpty();
	}

	@Test
	void refusesToSaveAcrossABlock() throws Exception {
		long id = writePost(hers, "unreachable");
		mockMvc.perform(asUser(post("/api/v1/users/anna/block"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(post("/api/v1/posts/" + id + "/bookmark"), mine))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnonymousAccess() throws Exception {
		mockMvc.perform(get("/api/v1/me/bookmarks")).andExpect(status().isUnauthorized());
	}
}
