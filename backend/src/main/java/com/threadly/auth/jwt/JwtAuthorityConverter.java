package com.threadly.auth.jwt;

import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps the {@code role} claim onto a Spring Security authority. The default converter looks at
 * {@code scope}/{@code scp}, which Threadly's own tokens do not use.
 */
@Component
public class JwtAuthorityConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		String role = jwt.getClaimAsString(AccessTokenService.ROLE_CLAIM);
		List<SimpleGrantedAuthority> authorities = role == null
				? List.of()
				: List.of(new SimpleGrantedAuthority("ROLE_" + role));

		return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
	}
}
