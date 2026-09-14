package com.threadly.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.threadly.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

class OpenApiIT extends ApiIntegrationTest {

	@Test
	void publishesTheApiDescriptionWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Threadly API"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void documentsEveryFeatureArea() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/posts']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/feed/following']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/users/{username}/follow']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/notifications']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/me/bookmarks']").exists());
	}

	@Test
	void servesTheSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}
}
