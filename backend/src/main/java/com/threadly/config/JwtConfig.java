package com.threadly.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.threadly.common.ratelimit.RateLimitProperties;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Symmetric HS256 signing. A single service issues and verifies the tokens, so a shared secret is
 * enough; an asymmetric key pair would only be needed if a separate service had to verify them.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class, RateLimitProperties.class})
public class JwtConfig {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	@Bean
	public SecretKeySpec jwtSigningKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(), HMAC_ALGORITHM);
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKeySpec signingKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKeySpec signingKey) {
		return NimbusJwtDecoder.withSecretKey(signingKey).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
