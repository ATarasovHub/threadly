package com.threadly.identity.google;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Turns a Google ID token into the few claims Threadly needs.
 *
 * <p>The signature, expiry, issuer and audience are checked by the {@code JwtDecoder} this is
 * given; what remains here is the one rule the decoder cannot express. An unverified email must
 * never be used to find an existing account: anyone able to create a Google account with an
 * address they do not control could otherwise take over the Threadly account using it.
 */
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier {

	private final GoogleJwtDecoderProvider decoders;

	/**
	 * @throws JwtException if the token is not a valid ID token for this application
	 */
	public GoogleAccount verify(String idToken) {
		Jwt jwt = decoders.decoder().decode(idToken);

		String subject = jwt.getSubject();
		if (subject == null || subject.isBlank()) {
			throw new JwtException("Google ID token has no subject");
		}

		String email = jwt.getClaimAsString("email");
		boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));
		String name = jwt.getClaimAsString("name");
		String picture = jwt.getClaimAsString("picture");

		return new GoogleAccount(subject, email, emailVerified, name, picture);
	}

	/**
	 * @param subject       Google's stable {@code sub}; the only field safe to key an account on
	 * @param emailVerified whether Google vouches for the address
	 */
	public record GoogleAccount(
			String subject,
			String email,
			boolean emailVerified,
			String name,
			String pictureUrl) {
	}

	/** Rejects tokens minted for a different OAuth client. */
	static OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
		return jwt -> {
			List<String> audience = jwt.getClaimAsStringList(JwtClaimNames.AUD);
			if (audience != null && audience.contains(clientId)) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(
					new OAuth2Error("invalid_token", "The ID token was not issued for this application", null));
		};
	}
}
