package com.threadly.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.follow.FollowRepository;
import com.threadly.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class BlockIT extends ApiIntegrationTest {

	@Autowired
	private FollowRepository follows;

	private String mine;
	private String hers;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
	}

	private void block(String handle, String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/" + handle + "/block"), asToken))
				.andExpect(status().isNoContent());
	}

	private void follow(String handle, String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/" + handle + "/follow"), asToken))
				.andExpect(status().isNoContent());
	}

	private void writePost(String asToken, String content) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts"), asToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void hidesTheProfileFromBothSides() throws Exception {
		block("anna", mine);

		// 404 rather than 403 in both directions: a distinct status would reveal who blocked whom.
		mockMvc.perform(asUser(get("/api/v1/users/anna"), mine)).andExpect(status().isNotFound());
		mockMvc.perform(asUser(get("/api/v1/users/andrii"), hers)).andExpect(status().isNotFound());
	}

	@Test
	void tearsDownFollowEdgesInBothDirections() throws Exception {
		follow("anna", mine);
		follow("andrii", hers);
		assertThat(follows.count()).isEqualTo(2);

		block("anna", mine);

		assertThat(follows.count()).isZero();
	}

	@Test
	void keepsBlockedPostsOutOfTheForYouFeed() throws Exception {
		writePost(hers, "from anna");
		writePost(mine, "from me");

		block("anna", mine);

		String feed = mockMvc.perform(asUser(get("/api/v1/feed/for-you"), mine))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(feed).contains("from me").doesNotContain("from anna");

		// And the other way round.
		String herFeed = mockMvc.perform(asUser(get("/api/v1/feed/for-you"), hers))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(herFeed).contains("from anna").doesNotContain("from me");
	}

	@Test
	void hidesTimelineAndSinglePosts() throws Exception {
		writePost(hers, "from anna");
		block("anna", mine);

		mockMvc.perform(asUser(get("/api/v1/users/anna/posts"), mine))
				.andExpect(status().isNotFound());
	}

	@Test
	void preventsFollowingAcrossABlock() throws Exception {
		block("anna", mine);

		mockMvc.perform(asUser(post("/api/v1/users/anna/follow"), mine))
				.andExpect(status().isNotFound());
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNotFound());
	}

	@Test
	void unblockingRestoresVisibilityButNotFollows() throws Exception {
		follow("anna", mine);
		block("anna", mine);

		mockMvc.perform(asUser(delete("/api/v1/users/anna/block"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(get("/api/v1/users/anna"), mine)).andExpect(status().isOk());
		// The follow was torn down by the block and is not resurrected by lifting it.
		assertThat(follows.count()).isZero();
	}

	@Test
	void blockingTwiceIsANoOp() throws Exception {
		block("anna", mine);
		block("anna", mine);

		mockMvc.perform(asUser(get("/api/v1/users/anna"), mine)).andExpect(status().isNotFound());
	}

	@Test
	void refusesSelfBlock() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/block"), mine))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAnonymousBlocking() throws Exception {
		mockMvc.perform(post("/api/v1/users/anna/block")).andExpect(status().isUnauthorized());
	}
}
