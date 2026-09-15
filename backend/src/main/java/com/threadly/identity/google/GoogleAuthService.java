package com.threadly.identity.google;

import com.threadly.auth.AuthService;
import com.threadly.common.error.BadRequestException;
import com.threadly.identity.IdentityProvider;
import com.threadly.identity.UserIdentity;
import com.threadly.identity.UserIdentityRepository;
import com.threadly.identity.google.GoogleIdTokenVerifier.GoogleAccount;
import com.threadly.user.Role;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import java.security.SecureRandom;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signs a user in with a Google ID token.
 *
 * <p>Google only ever proves who the caller is. Everything after that — the session, the access
 * token, the rotating refresh cookie — is Threadly's own, exactly as for a password sign-in. That
 * is why this returns the same {@link AuthService.AuthenticatedSession} the login endpoint does.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

	private static final int HANDLE_MAX_LENGTH = 30;
	private static final int SUFFIX_ATTEMPTS = 10;

	private final GoogleIdTokenVerifier verifier;
	private final GoogleProperties properties;
	private final UserIdentityRepository identities;
	private final UserRepository users;
	private final AuthService authService;
	private final SecureRandom random = new SecureRandom();

	@Transactional
	public AuthService.AuthenticatedSession signIn(String idToken) {
		if (!properties.enabled()) {
			throw new BadRequestException("Google sign-in is not configured on this server.");
		}

		GoogleAccount account;
		try {
			account = verifier.verify(idToken);
		}
		catch (JwtException e) {
			log.debug("Rejected a Google ID token: {}", e.getMessage());
			// Same shape as any other failed sign-in; the reason is not the caller's business.
			throw new BadCredentialsException("The Google sign-in could not be verified");
		}

		User user = identities.findByProviderAndSubject(IdentityProvider.GOOGLE, account.subject())
				.map(UserIdentity::getUser)
				.orElseGet(() -> linkOrCreate(account));

		if (!user.isEnabled()) {
			throw new BadCredentialsException("This account cannot sign in");
		}

		return authService.startSession(user);
	}

	/**
	 * First sign-in with this Google account: either attach it to an existing Threadly account
	 * with the same address, or create a new one.
	 *
	 * <p>Matching on the email is what makes "I signed up with a password, now I click Google"
	 * work instead of silently producing a second account. It is also the dangerous half: if the
	 * address were not verified by Google, anyone who could register that address at Google could
	 * claim the Threadly account. Hence the hard requirement below.
	 */
	private User linkOrCreate(GoogleAccount account) {
		if (account.email() == null || !account.emailVerified()) {
			throw new BadCredentialsException("Google has not verified this address");
		}

		User user = users.findByEmailIgnoreCase(account.email())
				.orElseGet(() -> createAccount(account));

		identities.save(UserIdentity.of(user, IdentityProvider.GOOGLE, account.subject(), account.email()));
		return user;
	}

	private User createAccount(GoogleAccount account) {
		String displayName = account.name() == null || account.name().isBlank()
				? account.email().split("@")[0]
				: account.name();

		return users.save(User.builder()
				.username(availableHandle(account.email()))
				.email(account.email())
				// No password: this account signs in through Google. Column is nullable as of V12.
				.passwordHash(null)
				.displayName(truncate(displayName, 50))
				.role(Role.USER)
				.enabled(true)
				.build());
	}

	/**
	 * Google supplies a name and an address, never a handle, so one has to be derived.
	 *
	 * <p>The derived handle is a starting point, not a decision: the account can change it from
	 * settings, which is why an unglamorous numeric suffix is acceptable here.
	 */
	private String availableHandle(String email) {
		String base = email.split("@")[0]
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9_]", "");
		if (base.length() < 3) {
			base = "user" + base;
		}
		base = truncate(base, HANDLE_MAX_LENGTH - 5);

		if (!users.existsByUsernameIgnoreCase(base)) {
			return base;
		}
		for (int attempt = 0; attempt < SUFFIX_ATTEMPTS; attempt++) {
			String candidate = base + random.nextInt(1000, 10000);
			if (!users.existsByUsernameIgnoreCase(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Could not derive a free handle from " + base);
	}

	private static String truncate(String value, int max) {
		return value.length() <= max ? value : value.substring(0, max);
	}
}
