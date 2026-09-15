package com.threadly.identity.google;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Supplies the decoder used for Google ID tokens.
 *
 * <p>This exists as its own bean so tests can replace it with a decoder backed by a local key
 * pair. Verifying against Google's live JWKS in a test would mean a network call and a token this
 * project cannot mint; substituting at this boundary keeps the whole sign-in path — claim
 * handling, account creation, linking — under test without either.
 *
 * <p>The decoder is built once and cached by Nimbus, which also refreshes Google's keys on its
 * own when they rotate.
 */
@Component
@RequiredArgsConstructor
public class GoogleJwtDecoderProvider {

	private final GoogleProperties properties;

	private volatile JwtDecoder decoder;

	public JwtDecoder decoder() {
		JwtDecoder current = decoder;
		if (current == null) {
			synchronized (this) {
				current = decoder;
				if (current == null) {
					current = build();
					decoder = current;
				}
			}
		}
		return current;
	}

	private JwtDecoder build() {
		NimbusJwtDecoder built = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
		built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
				// Expiry and not-before.
				JwtValidators.createDefault(),
				issuerValidator(),
				GoogleIdTokenVerifier.audienceValidator(properties.clientId()))));
		return built;
	}

	/**
	 * Google mints ID tokens under two spellings of the same issuer — with and without the
	 * scheme — so a single-value {@code JwtIssuerValidator} would reject half of them.
	 */
	private OAuth2TokenValidator<Jwt> issuerValidator() {
		List<String> accepted = properties.issuers();
		return jwt -> {
			String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
			if (issuer != null && accepted.contains(issuer)) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(
					new OAuth2Error("invalid_token", "Unexpected ID token issuer", null));
		};
	}
}
