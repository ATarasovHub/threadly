package com.threadly.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI threadlyOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Threadly API")
						.version("v1")
						.description("""
								A Twitter/X-style social network.

								Authentication is a short-lived JWT access token sent as
								`Authorization: Bearer <token>`. The refresh token lives only in an
								HttpOnly cookie and is rotated on every use, so it cannot be used
								from this page.

								Collections are paginated by cursor, never by offset: pass the
								`nextCursor` from the previous response back as `cursor`.

								Errors are RFC 7807 problem details.
								""")
						.license(new License().name("MIT")))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				// Applied to every operation; the endpoints that do not need it are the few
				// permitted in SecurityConfig.
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
	}
}
