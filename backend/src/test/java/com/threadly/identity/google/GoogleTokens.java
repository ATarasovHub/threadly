package com.threadly.identity.google;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

/**
 * Mints ID tokens the way Google would, signed with a key pair generated for the test run.
 *
 * <p>Verifying against Google's live keys in a test would need a network call and a token this
 * project has no way to obtain. Substituting the key pair — and, in the test configuration, the
 * decoder that trusts it — leaves everything downstream of the signature check under test:
 * audience, issuer, the verified-email rule, account creation and linking.
 */
public final class GoogleTokens {

	public static final String CLIENT_ID = "threadly-test-client.apps.googleusercontent.com";
	public static final String ISSUER = "https://accounts.google.com";

	private static final KeyPair KEY_PAIR = generateKeyPair();

	private GoogleTokens() {
	}

	public static RSAPublicKey publicKey() {
		return (RSAPublicKey) KEY_PAIR.getPublic();
	}

	public static Builder idToken(String subject) {
		return new Builder(subject);
	}

	public static final class Builder {

		private final String subject;
		private String audience = CLIENT_ID;
		private String issuer = ISSUER;
		private String email = "someone@example.com";
		private boolean emailVerified = true;
		private String name = "Someone";
		private Instant expiresAt = Instant.now().plusSeconds(600);

		private Builder(String subject) {
			this.subject = subject;
		}

		public Builder audience(String value) {
			this.audience = value;
			return this;
		}

		public Builder issuer(String value) {
			this.issuer = value;
			return this;
		}

		public Builder email(String value) {
			this.email = value;
			return this;
		}

		public Builder emailVerified(boolean value) {
			this.emailVerified = value;
			return this;
		}

		public Builder name(String value) {
			this.name = value;
			return this;
		}

		public Builder expiredAlready() {
			this.expiresAt = Instant.now().minusSeconds(60);
			return this;
		}

		public String sign() {
			try {
				JWTClaimsSet claims = new JWTClaimsSet.Builder()
						.subject(subject)
						.issuer(issuer)
						.audience(audience)
						.claim("email", email)
						.claim("email_verified", emailVerified)
						.claim("name", name)
						.issueTime(Date.from(Instant.now().minusSeconds(5)))
						.expirationTime(Date.from(expiresAt))
						.build();

				SignedJWT jwt = new SignedJWT(
						new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
				jwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
				return jwt.serialize();
			}
			catch (Exception e) {
				throw new IllegalStateException("Could not mint a test ID token", e);
			}
		}
	}

	/** The signing key as a JWK, for building a decoder that trusts it. */
	public static RSAKey jwk() {
		return new RSAKey.Builder(publicKey()).keyID("test-key").build();
	}

	private static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (Exception e) {
			throw new IllegalStateException("Could not generate a test key pair", e);
		}
	}
}
