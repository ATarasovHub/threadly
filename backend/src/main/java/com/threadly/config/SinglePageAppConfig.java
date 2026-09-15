package com.threadly.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React client from the same origin as the API.
 *
 * <p>One origin is not a convenience here, it is what keeps the security model intact. The refresh
 * cookie is {@code SameSite=Strict}, which is the reason CSRF protection can be switched off; on a
 * separate frontend domain the cookie would have to become {@code SameSite=None} and that reason
 * would evaporate. Serving both from one place avoids the trade entirely, and removes CORS from
 * production along the way.
 *
 * <p>Client-side routes such as {@code /andrii} have no file behind them, so anything that is not
 * a real asset falls back to {@code index.html} and React Router takes over. API, docs and
 * actuator paths are excluded: a wrong URL under {@code /api} must answer with a JSON 404, not
 * with a web page.
 */
@Configuration
public class SinglePageAppConfig implements WebMvcConfigurer {

	private static final String[] SERVER_PREFIXES = {"/api/", "/actuator", "/v3/api-docs", "/swagger-ui"};

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						if (isServerPath(resourcePath)) {
							return null;
						}
						ClassPathResource index = new ClassPathResource("static/index.html");
						// Absent in a backend-only build, where there is no client to serve.
						return index.exists() ? index : null;
					}
				});
	}

	private static boolean isServerPath(String resourcePath) {
		String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
		for (String prefix : SERVER_PREFIXES) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
