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

class PostReplyIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");
	private static final Pattern CONTENT = Pattern.compile("\"content\":\"([^\"]+)\"");
	private static final Pattern NEXT_CURSOR = Pattern.compile("\"nextCursor\":\"([^\"]+)\"");

	private String mine;
	private String hers;
	private long rootId;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
		rootId = publish("/api/v1/posts", mine, "Root post");
	}

	private long publish(String path, String asToken, String content) throws Exception {
		String body = mockMvc.perform(asUser(post(path), asToken)
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

	private long reply(String asToken, String content) throws Exception {
		return publish("/api/v1/posts/" + rootId + "/replies", asToken, content);
	}

	private static List<String> contentsOf(String body) {
		List<String> found = new ArrayList<>();
		Matcher matcher = CONTENT.matcher(body);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return found;
	}

	private String threadPage(String cursor, int limit) throws Exception {
		var request = get("/api/v1/posts/" + rootId + "/replies").param("limit", String.valueOf(limit));
		if (cursor != null) {
			request = request.param("cursor", cursor);
		}
		return mockMvc.perform(asUser(request, mine))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	@Test
	void publishesAReplyPointingAtItsParent() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + rootId + "/replies"), hers)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"Nice one\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Nice one"))
				.andExpect(jsonPath("$.inReplyTo.id").value((int) rootId))
				.andExpect(jsonPath("$.inReplyTo.authorUsername").value("andrii"));
	}

	@Test
	void countsRepliesOnTheParent() throws Exception {
		reply(hers, "one");
		reply(mine, "two");

		mockMvc.perform(asUser(get("/api/v1/posts/" + rootId), mine))
				.andExpect(jsonPath("$.metrics.replies").value(2))
				.andExpect(jsonPath("$.inReplyTo").doesNotExist());
	}

	@Test
	void readsAThreadOldestFirst() throws Exception {
		reply(hers, "first");
		reply(mine, "second");
		reply(hers, "third");

		// The one listing that runs forwards in time: a conversation starts at the beginning.
		assertThat(contentsOf(threadPage(null, 20)))
				.containsExactly("first", "second", "third");
	}

	@Test
	void walksAThreadForwardsAcrossPages() throws Exception {
		for (int i = 1; i <= 5; i++) {
			reply(hers, "reply-" + i);
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			String body = threadPage(cursor, 2);
			seen.addAll(contentsOf(body));
			Matcher matcher = NEXT_CURSOR.matcher(body);
			cursor = matcher.find() ? matcher.group(1) : null;
		}
		while (cursor != null);

		assertThat(seen).containsExactly("reply-1", "reply-2", "reply-3", "reply-4", "reply-5");
	}

	@Test
	void keepsRepliesOutOfFeeds() throws Exception {
		reply(hers, "a reply");

		String feed = mockMvc.perform(asUser(get("/api/v1/feed/for-you"), mine))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		// Replies belong to their thread, not to the timeline.
		assertThat(contentsOf(feed)).containsExactly("Root post");
	}

	@Test
	void keepsRepliesOutOfProfileTimelinesAndPostCounts() throws Exception {
		reply(mine, "my own reply");

		String timeline = mockMvc.perform(asUser(get("/api/v1/users/andrii/posts"), mine))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(contentsOf(timeline)).containsExactly("Root post");

		mockMvc.perform(asUser(get("/api/v1/users/andrii"), mine))
				.andExpect(jsonPath("$.stats.posts").value(1));
	}

	@Test
	void repliesCanBeLikedLikeAnyPost() throws Exception {
		long replyId = reply(hers, "likeable");

		mockMvc.perform(asUser(post("/api/v1/posts/" + replyId + "/like"), mine))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(get("/api/v1/posts/" + replyId), mine))
				.andExpect(jsonPath("$.metrics.likes").value(1));
	}

	@Test
	void deletingAReplyRemovesItFromTheThreadAndTheCount() throws Exception {
		long replyId = reply(hers, "temporary");

		mockMvc.perform(asUser(delete("/api/v1/posts/" + replyId), hers))
				.andExpect(status().isNoContent());

		assertThat(contentsOf(threadPage(null, 20))).isEmpty();
		mockMvc.perform(asUser(get("/api/v1/posts/" + rootId), mine))
				.andExpect(jsonPath("$.metrics.replies").value(0));
	}

	@Test
	void refusesToReplyAcrossABlock() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/anna/block"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(post("/api/v1/posts/" + rootId + "/replies"), hers)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"blocked\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void answersReplyToUnknownPostWithNotFound() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/999999/replies"), mine)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"nowhere\"}"))
				.andExpect(status().isNotFound());
	}
}
