package com.threadly.profile;

import com.threadly.profile.dto.ProfileResponse;
import com.threadly.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profileService;

	@GetMapping("/users/{username}")
	public ProfileResponse profile(@PathVariable String username) {
		return profileService.findByUsername(username);
	}

	@PatchMapping("/me/profile")
	public ProfileResponse updateOwnProfile(@Valid @RequestBody UpdateProfileRequest request) {
		return profileService.updateOwnProfile(request);
	}
}
