package com.threadly.post;

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

class PostTimelineIT extends ApiIntegrationTest {

	private static final Pattern CONTENT = Pattern.compile("\"content\":\"([^\"]+)\"");
	private static final Pattern NEXT_CURSOR = Pattern.compile("\"nextCursor\":\"([^\"]+)\"");

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		token = accessTokenFor("andrii");
	}

	private void writePost(String content) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts"), token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"%s\"}".formatted(content)))
				.andExpect(status().isCreated());
	}

	private String timeline(String cursor, int limit) throws Exception {
		var request = get("/api/v1/users/andrii/posts").param("limit", String.valueOf(limit));
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
	void ordersTimelineNewestFirst() throws Exception {
		writePost("first");
		writePost("second");
		writePost("third");

		assertThat(contentsOf(timeline(null, 20))).containsExactly("third", "second", "first");
	}

	@Test
	void walksEveryPostExactlyOnceAcrossPages() throws Exception {
		for (int i = 1; i <= 7; i++) {
			writePost("post-" + i);
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			String body = timeline(cursor, 3);
			seen.addAll(contentsOf(body));
			cursor = nextCursorOf(body);
		}
		while (cursor != null);

		assertThat(seen).containsExactly(
				"post-7", "post-6", "post-5", "post-4", "post-3", "post-2", "post-1");
	}

	@Test
	void reportsNoCursorOnTheLastPage() throws Exception {
		writePost("only");

		String body = timeline(null, 20);
		assertThat(nextCursorOf(body)).isNull();
	}

	@Test
	void doesNotRepeatARowWhenPostsArriveMidPagination() throws Exception {
		for (int i = 1; i <= 4; i++) {
			writePost("post-" + i);
		}

		String firstPage = timeline(null, 2);
		assertThat(contentsOf(firstPage)).containsExactly("post-4", "post-3");

		// A new post lands between the two requests. With offset paging this would push the
		// window down and serve post-3 a second time; a cursor pins the position instead.
		writePost("post-5");

		String secondPage = timeline(nextCursorOf(firstPage), 2);
		assertThat(contentsOf(secondPage)).containsExactly("post-2", "post-1");
	}

	@Test
	void leavesDeletedPostsOutOfTheTimeline() throws Exception {
		writePost("keep");
		writePost("remove");

		String body = timeline(null, 20);
		long removedId = Long.parseLong(
				body.replaceAll(".*?\\{\"id\":(\\d+),\"content\":\"remove\".*", "$1"));
		mockMvc.perform(asUser(delete("/api/v1/posts/" + removedId), token))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(timeline(null, 20))).containsExactly("keep");
	}

	@Test
	void rejectsAFabricatedCursor() throws Exception {
		mockMvc.perform(asUser(get("/api/v1/users/andrii/posts").param("cursor", "not-a-cursor"), token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid cursor"));
	}

	@Test
	void rejectsAnOversizedPageRequest() throws Exception {
		mockMvc.perform(asUser(get("/api/v1/users/andrii/posts").param("limit", "1000"), token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void answersUnknownHandleWithNotFound() throws Exception {
		mockMvc.perform(asUser(get("/api/v1/users/ghost/posts"), token))
				.andExpect(status().isNotFound());
	}
}
