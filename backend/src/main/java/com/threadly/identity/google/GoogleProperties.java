package com.threadly.identity.google;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param clientId   the OAuth client id this application was issued; an ID token minted for some
 *                   other application must be rejected, which is what the audience check is for
 * @param issuers    accepted {@code iss} values — Google mints tokens under both spellings
 * @param jwkSetUri  where Google publishes the keys its ID tokens are signed with
 * @param enabled    false when no client id is configured, which turns the endpoint off rather
 *                   than letting it accept anything
 */
@ConfigurationProperties(prefix = "threadly.oauth.google")
public record GoogleProperties(String clientId, List<String> issuers, String jwkSetUri) {

	public GoogleProperties {
		issuers = issuers == null || issuers.isEmpty()
				? List.of("https://accounts.google.com", "accounts.google.com")
				: issuers;
		jwkSetUri = jwkSetUri == null || jwkSetUri.isBlank()
				? "https://www.googleapis.com/oauth2/v3/certs"
				: jwkSetUri;
	}

	public boolean enabled() {
		return clientId != null && !clientId.isBlank();
	}
}
