package com.threadly.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.http.MediaType;

class NotificationIT extends ApiIntegrationTest {

	private static final Pattern POST_ID = Pattern.compile("[{,]\"id\":(\\d+)");

	@Autowired
	private NotificationRepository notifications;

	private String mine;
	private String hers;

	@BeforeEach
	void signIn() throws Exception {
		notifications.deleteAll();
		givenAccount("andrii");
		givenAccount("anna");
		mine = accessTokenFor("andrii");
		hers = accessTokenFor("anna");
	}

	private long writePost(String asToken, String content) throws Exception {
		String body = mockMvc.perform(asUser(post("/api/v1/posts"), asToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"" + content + "\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		Matcher matcher = POST_ID.matcher(body);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}

	/** Notifications are written after the triggering transaction commits, so they land shortly after. */
	private void settle(int expected) {
		await().atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> assertThat(notifications.count()).isEqualTo(expected));
	}

	@Test
	void recordsAFollow() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());

		settle(1);
		// Read through the inbox query, which join-fetches the actor: findAll() would hand back
		// lazy proxies with no session to initialise them.
		Long recipientId = users.findByUsernameIgnoreCase("andrii").orElseThrow().getId();
		assertThat(notifications.findInbox(recipientId, Limit.of(10)))
				.singleElement()
				.satisfies(n -> {
					assertThat(n.getType()).isEqualTo(NotificationType.FOLLOW);
					assertThat(n.getActor().getUsername()).isEqualTo("anna");
					assertThat(n.getPost()).isNull();
				});
	}

	@Test
	void recordsALikeWithThePost() throws Exception {
		long postId = writePost(mine, "Hello");

		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNoContent());

		settle(1);
		Long recipientId = users.findByUsernameIgnoreCase("andrii").orElseThrow().getId();
		assertThat(notifications.findInbox(recipientId, Limit.of(10)))
				.singleElement()
				.satisfies(n -> {
					assertThat(n.getType()).isEqualTo(NotificationType.LIKE);
					assertThat(n.getPost().getId()).isEqualTo(postId);
				});
	}

	@Test
	void recordsRepliesRepostsAndQuotesSeparately() throws Exception {
		long postId = writePost(mine, "Hello");

		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/replies"), hers)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"nice\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/repost"), hers))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/quote"), hers)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"look\"}"))
				.andExpect(status().isCreated());

		settle(3);
		Long recipientId = users.findByUsernameIgnoreCase("andrii").orElseThrow().getId();
		assertThat(notifications.findInbox(recipientId, Limit.of(10)))
				.extracting(Notification::getType)
				.containsExactlyInAnyOrder(
						NotificationType.REPLY, NotificationType.REPOST, NotificationType.QUOTE);
	}

	@Test
	void staysSilentAboutYourOwnActions() throws Exception {
		long postId = writePost(mine, "Hello");

		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), mine))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/repost"), mine))
				.andExpect(status().isNoContent());

		// Liking your own post is allowed; being told about it is not useful.
		await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(4))
				.untilAsserted(() -> assertThat(notifications.count()).isZero());
	}

	@Test
	void leavesNoNotificationWhenTheActionWasIdempotentlyIgnored() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());
		settle(1);

		// The second follow changes nothing, so it must not notify again.
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());

		await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(4))
				.untilAsserted(() -> assertThat(notifications.count()).isEqualTo(1));
	}

	@Test
	void servesTheInboxNewestFirstWithUnreadCount() throws Exception {
		long postId = writePost(mine, "Hello");
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNoContent());
		settle(2);

		mockMvc.perform(asUser(get("/api/v1/notifications"), mine))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].type").value("LIKE"))
				.andExpect(jsonPath("$.items[0].actor.username").value("anna"))
				.andExpect(jsonPath("$.items[0].post.excerpt").value("Hello"))
				.andExpect(jsonPath("$.items[0].read").value(false))
				.andExpect(jsonPath("$.items[1].type").value("FOLLOW"))
				.andExpect(jsonPath("$.items[1].post").doesNotExist());

		mockMvc.perform(asUser(get("/api/v1/notifications/unread-count"), mine))
				.andExpect(jsonPath("$.unread").value(2));
	}

	@Test
	void marksTheInboxRead() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());
		settle(1);

		mockMvc.perform(asUser(post("/api/v1/notifications/read"), mine))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.marked").value(1));

		mockMvc.perform(asUser(get("/api/v1/notifications/unread-count"), mine))
				.andExpect(jsonPath("$.unread").value(0));
	}

	@Test
	void keepsInboxesSeparate() throws Exception {
		mockMvc.perform(asUser(post("/api/v1/users/andrii/follow"), hers))
				.andExpect(status().isNoContent());
		settle(1);

		mockMvc.perform(asUser(get("/api/v1/notifications/unread-count"), hers))
				.andExpect(jsonPath("$.unread").value(0));
	}

	@Test
	void survivesTheReferencedPostBeingDeleted() throws Exception {
		long postId = writePost(mine, "Hello");
		mockMvc.perform(asUser(post("/api/v1/posts/" + postId + "/like"), hers))
				.andExpect(status().isNoContent());
		settle(1);

		mockMvc.perform(asUser(delete("/api/v1/posts/" + postId), mine))
				.andExpect(status().isNoContent());

		mockMvc.perform(asUser(get("/api/v1/notifications"), mine))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsAnonymousAccess() throws Exception {
		mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
	}
}
