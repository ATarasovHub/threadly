package com.threadly.auth.jwt;

import com.threadly.config.JwtProperties;
import com.threadly.user.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues the short-lived access tokens clients send on every request. */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

	/** Carries the role so authorities can be resolved without a database lookup per request. */
	public static final String ROLE_CLAIM = "role";

	/** Carries the handle so the API can greet the caller without a lookup either. */
	public static final String USERNAME_CLAIM = "username";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;

	public IssuedToken issue(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				// The account id is the stable identity; handles can be changed by their owner.
				.subject(String.valueOf(user.getId()))
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim(USERNAME_CLAIM, user.getUsername())
				.claim(ROLE_CLAIM, user.getRole().name())
				.build();

		// The encoder defaults to RS256; the header has to name HS256 to match the shared secret.
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(value, expiresAt, properties.accessTokenTtl().toSeconds());
	}

	public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
	}
}
