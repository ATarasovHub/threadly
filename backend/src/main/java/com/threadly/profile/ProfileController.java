package com.threadly.profile;

import com.threadly.profile.dto.ProfileResponse;
import com.threadly.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	@PatchMapping("/me/profile")
	public ProfileResponse updateOwnProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return profileService.updateOwnProfile(request);
	}
}
