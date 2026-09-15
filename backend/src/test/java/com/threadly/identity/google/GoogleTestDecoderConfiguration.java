package com.threadly.identity.google;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces the Google decoder with one that trusts the test key pair.
 *
 * <p>Only the source of the signing key changes. The validators are the production ones —
 * expiry, issuer and audience — so a token with the wrong audience or a stale expiry is still
 * rejected here for the same reason it would be in production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class GoogleTestDecoderConfiguration {

	@Bean
	@Primary
	GoogleJwtDecoderProvider testGoogleDecoderProvider(GoogleProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withPublicKey(GoogleTokens.publicKey())
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
				JwtValidators.createDefault(),
				issuerValidator(properties.issuers()),
				GoogleIdTokenVerifier.audienceValidator(properties.clientId()))));

		return new GoogleJwtDecoderProvider(properties) {
			@Override
			public JwtDecoder decoder() {
				return decoder;
			}
		};
	}

	private static OAuth2TokenValidator<Jwt> issuerValidator(List<String> accepted) {
		return jwt -> {
			String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
			return issuer != null && accepted.contains(issuer)
					? OAuth2TokenValidatorResult.success()
					: OAuth2TokenValidatorResult.failure(
							new OAuth2Error("invalid_token", "Unexpected ID token issuer", null));
		};
	}
}
