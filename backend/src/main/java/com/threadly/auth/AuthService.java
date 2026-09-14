package com.threadly.auth;

import com.threadly.auth.dto.AuthenticationResponse;
import com.threadly.auth.dto.LoginRequest;
import com.threadly.auth.dto.RegisterRequest;
import com.threadly.auth.jwt.AccessTokenService;
import com.threadly.common.error.DuplicateResourceException;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import com.threadly.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final AccessTokenService accessTokenService;

	/**
	 * Creates an account. The pre-checks give callers a precise error; the unique indexes from
	 * migration V1 remain the actual guarantee under concurrent registrations.
	 */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (users.existsByUsernameIgnoreCase(request.username())) {
			throw new DuplicateResourceException("username", "Handle @%s is already taken.".formatted(request.username()));
		}
		if (users.existsByEmailIgnoreCase(request.email())) {
			throw new DuplicateResourceException("email", "That email address is already registered.");
		}

		User user = User.builder()
				.username(request.username())
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.displayName(request.displayName())
				.role(Role.USER)
				.enabled(true)
				.build();

		return UserResponse.from(users.save(user));
	}

	/**
	 * Verifies the credentials and mints an access token.
	 *
	 * <p>Delegating to the {@link AuthenticationManager} keeps bad credentials, disabled and
	 * unknown accounts indistinguishable to the caller, which is what stops handle enumeration.
	 */
	@Transactional(readOnly = true)
	public AuthenticationResponse login(LoginRequest request) {
		var authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));

		String username = ((UserDetails) authentication.getPrincipal()).getUsername();
		User user = users.findByUsernameIgnoreCase(username).orElseThrow();

		AccessTokenService.IssuedToken token = accessTokenService.issue(user);
		return AuthenticationResponse.bearer(token.value(), token.expiresInSeconds(), UserResponse.from(user));
	}
}
