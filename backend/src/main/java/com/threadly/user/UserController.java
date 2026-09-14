package com.threadly.user;

import com.threadly.user.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

	private final CurrentUserService currentUserService;

	@GetMapping("/me")
	public CurrentUserResponse me() {
		return CurrentUserResponse.from(currentUserService.require());
	}
}
