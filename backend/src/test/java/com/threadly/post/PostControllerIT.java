package com.threadly.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.threadly.support.ApiIntegrationTest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class PostControllerIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");

	@Autowired
	private PostRepository posts;

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		givenAccount("andrii");
		token = accessTokenFor("andrii");
	}

	private long writePost(String content, String asToken) throws Exception {
		MvcResult result = mockMvc.perform(asUser(post("/api/v1/posts"), asToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"%s\"}".formatted(content)))
				.andExpect(status().isCreated())
				.andReturn();
		// The first "id" in the body is the post's; a greedy match would pick up the author's.
		Matcher matcher = POST_ID.matcher(result.getResponse().getContentAsString());
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}

	private long writePost(String content) throws Exception {
		return writePost(content, token);
	}

	@Test
	void publishesAPost() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts"), token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"  Hello Threadly  "}
								"""))
				.andExpect(status().isCreated())
				// Surrounding whitespace is stripped before storing.
				.andExpect(jsonPath("$.content").value("Hello Threadly"))
				.andExpect(jsonPath("$.author.username").value("andrii"))
				.andExpect(jsonPath("$.edited").value(false))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void rejectsBlankOrOverlongContent() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/posts"), token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"   "}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(asUser(post("/api/v1/posts"), token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"%s\"}".formatted("x".repeat(501))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.content").value("must be at most 500 characters"));
	}

	@Test
	void rejectsAnonymousPublishing() throws Exception {
		mockMvc.perform(post("/api/v1/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Hello"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void marksAPostAsEditedAfterAChange() throws Exception {
		long id = writePost("First draft");

		mockMvc.perform(asUser(patch("/api/v1/posts/" + id), token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Second draft"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Second draft"))
				.andExpect(jsonPath("$.edited").value(true));
	}

	@Test
	void refusesToEditSomeoneElsesPost() throws Exception {
		long id = writePost("Mine");
		givenAccount("anna");
		String annasToken = accessTokenFor("anna");

		mockMvc.perform(asUser(patch("/api/v1/posts/" + id), annasToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Not yours"}
								"""))
				// 403, not 404: the post is visible to Anna, she just may not change it.
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void deletesOwnPostAndStopsServingIt() throws Exception {
		long id = writePost("Temporary");

		mockMvc.perform(asUser(delete("/api/v1/posts/" + id), token))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(get("/api/v1/posts/" + id), token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletionIsSoftSoTheRowSurvives() throws Exception {
		long id = writePost("Temporary");
		mockMvc.perform(asUser(delete("/api/v1/posts/" + id), token))
				.andExpect(status().isNoContent());

		// The row is still there for replies and reposts to point at; it is just not served.
		assertThat(posts.findById(id)).get().matches(Post::isDeleted);
	}

	@Test
	void answersUnknownPostWithNotFound() throws Exception {
		mockMvc.perform(asUser(get("/api/v1/posts/999999"), token))
				.andExpect(status().isNotFound());
	}
}
