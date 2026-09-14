package com.threadly.auth.refresh;

import com.threadly.config.AuthProperties;
import com.threadly.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and rotates refresh tokens.
 *
 * <p>Each refresh consumes the presented token and returns a new one from the same family. If a
 * token that was already rotated away shows up again, the copy has been stolen — one of the two
 * holders is an attacker and there is no way to tell which — so the entire family is revoked and
 * both are forced to log in again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository tokens;
	private final RefreshTokenFamilyRevoker familyRevoker;
	private final AuthProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	/** Starts a new session family, used on login. */
	@Transactional
	public IssuedRefreshToken issue(User user) {
		return persist(user, UUID.randomUUID());
	}

	/**
	 * Validates the presented token and replaces it with a successor.
	 *
	 * @throws BadCredentialsException if the token is unknown, expired, or already rotated away
	 */
	@Transactional
	public IssuedRefreshToken rotate(String presentedToken) {
		Instant now = Instant.now();
		RefreshToken stored = tokens.findByTokenHash(hash(presentedToken))
				.orElseThrow(() -> new BadCredentialsException("Unknown refresh token"));

		if (stored.isRevoked()) {
			log.warn("Refresh token reuse detected for family {}; revoking the whole family", stored.getFamilyId());
			familyRevoker.revoke(stored.getFamilyId(), now);
			throw new BadCredentialsException("Refresh token was already used");
		}
		if (stored.isExpired(now)) {
			throw new BadCredentialsException("Refresh token expired");
		}

		stored.revoke(now);
		return persist(stored.getUser(), stored.getFamilyId());
	}

	/** Ends the session the token belongs to. Unknown tokens are ignored: logout is idempotent. */
	@Transactional
	public void revoke(String presentedToken) {
		tokens.findByTokenHash(hash(presentedToken))
				.ifPresent(stored -> familyRevoker.revoke(stored.getFamilyId(), Instant.now()));
	}

	public Duration ttl() {
		return properties.refreshTokenTtl();
	}

	private IssuedRefreshToken persist(User user, UUID familyId) {
		byte[] raw = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(raw);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());

		tokens.save(RefreshToken.builder()
				.user(user)
				.tokenHash(hash(value))
				.familyId(familyId)
				.expiresAt(expiresAt)
				.build());

		return new IssuedRefreshToken(value, user, expiresAt);
	}

	private static String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required by every JVM", e);
		}
	}

	public record IssuedRefreshToken(String value, User user, Instant expiresAt) {
	}
}
