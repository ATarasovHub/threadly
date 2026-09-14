package com.threadly.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PostLikeIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");

	@Autowired
	private PostLikeRepository likes;

	private String mine;
	private String hers;
	private long postId;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
		postId = writePost(mine, "Hello Threadly");
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

	private void like(String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), asToken))
				.andExpect(status().isNoContent());
	}

	@Test
	void startsWithNoLikes() throws Exception {
		mockMvc.perform(asUser(get("/api/v1/posts/" + postId), mine))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.metrics.likes").value(0))
				.andExpect(jsonPath("$.viewer.liked").value(false));
	}

	@Test
	void countsLikesAndReportsViewerState() throws Exception {
		like(hers);

		// Anna liked it: the count is public, the flag is per viewer.
		mockMvc.perform(asUser(get("/api/v1/posts/" + postId), hers))
				.andExpect(jsonPath("$.metrics.likes").value(1))
				.andExpect(jsonPath("$.viewer.liked").value(true));

		mockMvc.perform(asUser(get("/api/v1/posts/" + postId), mine))
				.andExpect(jsonPath("$.metrics.likes").value(1))
				.andExpect(jsonPath("$.viewer.liked").value(false));
	}

	@Test
	void likingTwiceCountsOnce() throws Exception {
		like(hers);
		like(hers);

		assertThat(likes.count()).isEqualTo(1);
		mockMvc.perform(asUser(get("/api/v1/posts/" + postId), hers))
				.andExpect(jsonPath("$.metrics.likes").value(1));
	}

	@Test
	void unlikeRemovesTheLikeAndIsIdempotent() throws Exception {
		like(hers);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(delete("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNoContent());

		assertThat(likes.count()).isZero();
	}

	@Test
	void lettingAnAuthorLikeTheirOwnPostIsAllowed() throws Exception {
		like(mine);

		mockMvc.perform(asUser(get("/api/v1/posts/" + postId), mine))
				.andExpect(jsonPath("$.viewer.liked").value(true));
	}

	@Test
	void surfacesLikesInTheFeed() throws Exception {
		like(hers);

		mockMvc.perform(asUser(get("/api/v1/feed/for-you"), hers))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].metrics.likes").value(1))
				.andExpect(jsonPath("$.items[0].viewer.liked").value(true));
	}

	@Test
	void dropsLikesWhenThePostIsDeleted() throws Exception {
		like(hers);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + postId), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNotFound());
	}

	@Test
	void refusesToLikeAcrossABlock() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/anna/block"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnonymousLiking() throws Exception {
		mockMvc.perform(post("/api/v1/posts/" + postId + "/like"))
				.andExpect(status().isUnauthorized());
	}
}
