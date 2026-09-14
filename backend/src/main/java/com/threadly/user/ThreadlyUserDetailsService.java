package com.threadly.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads accounts for password authentication; login accepts either a handle or an email. */
@Service
@RequiredArgsConstructor
public class ThreadlyUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		User user = users.findByUsernameIgnoreCase(identifier)
				.or(() -> users.findByEmailIgnoreCase(identifier))
				.orElseThrow(() -> new UsernameNotFoundException("No account for " + identifier));

		return org.springframework.security.core.userdetails.User
				.withUsername(user.getUsername())
				.password(user.getPasswordHash())
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
				.disabled(!user.isEnabled())
				.build();
	}
}
