package com.threadly.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the account behind the access token of the request being handled. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

	private final UserRepository users;

	@Transactional(readOnly = true)
	public User require() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			throw new AuthenticationCredentialsNotFoundException("No authenticated request in scope");
		}

		long accountId = Long.parseLong(token.getToken().getSubject());
		// A token outlives the account it was minted for; deleting an account must end its sessions.
		return users.findById(accountId)
				.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account no longer exists"));
	}
}
