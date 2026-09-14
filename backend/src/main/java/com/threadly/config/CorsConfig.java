package com.threadly.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cross-origin access for the browser client, which is served from its own origin in development.
 *
 * <p>Origins are listed explicitly rather than wildcarded: the refresh cookie is sent with
 * credentials, and a wildcard origin is both refused by browsers in that mode and wrong here
 * anyway — any site could then drive the API as the signed-in user.
 */
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
		configuration.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"));
		// Required for the refresh cookie to travel at all.
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	/**
	 * @param allowedOrigins exact origins permitted to call the API from a browser
	 */
	@ConfigurationProperties(prefix = "threadly.cors")
	public record CorsProperties(List<String> allowedOrigins) {

		public CorsProperties {
			allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
					? List.of("http://localhost:5173")
					: allowedOrigins;
		}
	}
}
