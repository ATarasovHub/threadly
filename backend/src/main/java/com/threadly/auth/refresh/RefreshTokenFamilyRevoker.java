package com.threadly.auth.refresh;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a token family in its own transaction.
 *
 * <p>Reuse detection has to reject the request and keep the revocation. Doing both in the caller's
 * transaction cannot work: the rejection unwinds it and takes the revocation with it, leaving the
 * stolen token usable. A separate transaction commits the revocation before the failure surfaces.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenFamilyRevoker {

	private final RefreshTokenRepository tokens;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int revoke(UUID familyId, Instant now) {
		return tokens.revokeFamily(familyId, now);
	}
}
