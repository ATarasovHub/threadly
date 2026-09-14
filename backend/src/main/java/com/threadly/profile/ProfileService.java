package com.threadly.profile;

import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.profile.dto.ProfileResponse;
import com.threadly.profile.dto.UpdateProfileRequest;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final CurrentUserService currentUserService;
	private final UserRepository users;

	/**
	 * Looks up a profile by handle, case-insensitively, so {@code /andrii} and {@code /Andrii}
	 * reach the same account.
	 */
	@Transactional(readOnly = true)
	public ProfileResponse findByUsername(String username) {
		User user = users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));
		return ProfileResponse.from(user);
	}

	@Transactional
	public ProfileResponse updateOwnProfile(UpdateProfileRequest request) {
		User user = currentUserService.require();
		user.updateProfile(
				request.displayName(),
				request.bio(),
				request.location(),
				request.website(),
				request.avatarUrl(),
				request.bannerUrl());
		// The entity is managed inside this transaction, so the update is flushed on commit.
		return ProfileResponse.from(user);
	}
}
