package com.threadly.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.TestcontainersConfiguration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Runs with a deliberately tiny quota so the limit is reachable in a test without sending
 * hundreds of requests.
 */
@SpringBootTest(properties = {
		"threadly.rate-limit.enabled=true",
		"threadly.rate-limit.capacity=5",
		"threadly.rate-limit.write-capacity=3",
		"threadly.rate-limit.period=1m"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RateLimitIT {

	private static final AtomicInteger CLIENTS = new AtomicInteger();

	@Autowired
	private MockMvc mockMvc;

	private String address;

	/**
	 * Quotas are keyed by client, and every test in this class would otherwise share one. Giving
	 * each test its own address keeps them independent without restarting the context.
	 */
	@BeforeEach
	void useAFreshClientAddress() {
		address = "10.0.0." + CLIENTS.incrementAndGet();
	}

	private RequestPostProcessor asClient() {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	private MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder request) {
		return request.with(asClient());
	}

	@Test
	void rejectsOnceTheReadQuotaIsSpent() throws Exception {
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(from(get("/api/v1/me"))).andExpect(status().isUnauthorized());
		}

		// The sixth request never reaches the endpoint.
		mockMvc.perform(from(get("/api/v1/me")))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.title").value("Too many requests"))
				.andExpect(jsonPath("$.type").value("https://threadly.dev/problems/rate-limited"));
	}

	@Test
	void countsWritesAgainstATighterQuotaThanReads() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(from(post("/api/v1/auth/login"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"identifier\":\"ghost\",\"password\":\"whatever1\"}"))
					.andExpect(status().isUnauthorized());
		}

		// Writes run out after three even though the read quota is five.
		mockMvc.perform(from(post("/api/v1/auth/login"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"identifier\":\"ghost\",\"password\":\"whatever1\"}"))
				.andExpect(status().isTooManyRequests());

		mockMvc.perform(from(get("/api/v1/me"))).andExpect(status().isUnauthorized());
	}

	@Test
	void reportsTheRemainingQuotaOnEveryAllowedRequest() throws Exception {
		var result = mockMvc.perform(from(get("/api/v1/me")))
				.andExpect(header().string("X-RateLimit-Limit", "5"))
				.andExpect(header().exists("X-RateLimit-Remaining"))
				.andReturn();

		assertThat(Integer.parseInt(result.getResponse().getHeader("X-RateLimit-Remaining")))
				.isLessThan(5);
	}

	@Test
	void leavesHealthChecksAndApiDocsUnmetered() throws Exception {
		for (int i = 0; i < 12; i++) {
			mockMvc.perform(from(get("/actuator/health"))).andExpect(status().isOk());
			mockMvc.perform(from(get("/v3/api-docs"))).andExpect(status().isOk());
		}
	}
}
