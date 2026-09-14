package com.threadly.auth;

import com.threadly.auth.dto.RegisterRequest;
import com.threadly.common.error.DuplicateResourceException;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import com.threadly.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

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
}
