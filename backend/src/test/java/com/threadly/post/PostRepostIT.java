package com.threadly.post;

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
import org.springframework.http.MediaType;

class PostRepostIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");

	private String mine;
	private String hers;
	private long originalId;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
		originalId = writePost(hers, "Original by Anna");
	}

	private long writePost(String asToken, String content) throws Exception {
		return idOf(mockMvc.perform(asUser(post("/api/v1/posts"), asToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString());
	}

	private static long idOf(String body) {
		Matcher matcher = POST_ID.matcher(body);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}

	private void repost(String asToken) throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + originalId + "/repost"), asToken))
				.andExpect(status().isNoContent());
	}

	@Test
	void repostAppearsInTheFeedCarryingTheOriginal() throws Exception {
		repost(mine);

		mockMvc.perform(asUser(get("/api/v1/users/andrii/posts"), mine))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].author.username").value("andrii"))
				// A plain repost has no words of its own.
				.andExpect(jsonPath("$.items[0].content").doesNotExist())
				.andExpect(jsonPath("$.items[0].repostOf.content").value("Original by Anna"))
				.andExpect(jsonPath("$.items[0].repostOf.author.username").value("anna"));
	}

	@Test
	void countsRepostsOnTheOriginalAndReportsViewerState() throws Exception {
		repost(mine);

		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(jsonPath("$.metrics.reposts").value(1))
				.andExpect(jsonPath("$.viewer.reposted").value(true));

		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), hers))
				.andExpect(jsonPath("$.metrics.reposts").value(1))
				.andExpect(jsonPath("$.viewer.reposted").value(false));
	}

	@Test
	void repostingTwiceCountsOnce() throws Exception {
		repost(mine);
		repost(mine);

		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(jsonPath("$.metrics.reposts").value(1));
	}

	@Test
	void undoingARepostIsIdempotent() throws Exception {
		repost(mine);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + originalId + "/repost"), mine))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(delete("/api/v1/posts/" + originalId + "/repost"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(jsonPath("$.metrics.reposts").value(0))
				.andExpect(jsonPath("$.viewer.reposted").value(false));
	}

	@Test
	void quotePostCarriesItsOwnText() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + originalId + "/quote"), mine)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"Worth reading\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Worth reading"))
				.andExpect(jsonPath("$.repostOf.content").value("Original by Anna"));
	}

	@Test
	void theSamePostCanBeQuotedMoreThanOnce() throws Exception {
		for (String comment : new String[] {"first thought", "second thought"}) {
			mockMvc.perform(asUser(post("/api/v1/posts/" + originalId + "/quote"), mine)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{" + "\"content\":\"" + comment + "\"}"))
					.andExpect(status().isCreated());
		}

		// Quoting is not a toggle: different commentary on the same post is legitimate.
		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(jsonPath("$.metrics.reposts").value(2));
	}

	@Test
	void aQuotePostDoesNotSetTheRepostedFlag() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts/" + originalId + "/quote"), mine)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{" + "\"content\":\"Worth reading\"}"))
				.andExpect(status().isCreated());

		// The repost button is still untoggled: quoting and reposting are separate actions.
		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(jsonPath("$.viewer.reposted").value(false));
	}

	@Test
	void deletingTheOriginalRemovesItsReposts() throws Exception {
		repost(mine);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + originalId), hers))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(get("/api/v1/posts/" + originalId), mine))
				.andExpect(status().isNotFound());
	}

	@Test
	void refusesToRepostAcrossABlock() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/anna/block"), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(post("/api/v1/posts/" + originalId + "/repost"), mine))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnonymousReposting() throws Exception {
		mockMvc.perform(post("/api/v1/posts/" + originalId + "/repost"))
				.andExpect(status().isUnauthorized());
	}
}
