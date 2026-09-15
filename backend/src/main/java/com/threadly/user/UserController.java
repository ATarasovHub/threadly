package com.threadly.user;

import com.threadly.user.dto.ChangeUsernameRequest;
import com.threadly.user.dto.CurrentUserResponse;
import com.threadly.common.error.DuplicateResourceException;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

	private final CurrentUserService currentUserService;
	private final UserRepository users;

	@GetMapping("/me")
	public CurrentUserResponse me() {
		return CurrentUserResponse.from(currentUserService.require());
	}

	/**
	 * Changes the caller's handle.
	 *
	 * <p>Needed above all by accounts created through Google, whose handle was derived from an
	 * email address rather than chosen. Old links break, which is why access tokens carry the
	 * account id and not the handle.
	 */
	@PatchMapping("/me/username")
	@Transactional
	public CurrentUserResponse changeUsername(@Valid @RequestBody ChangeUsernameRequest request) {
		User me = currentUserService.require();

		if (!me.getUsername().equalsIgnoreCase(request.username())
				&& users.existsByUsernameIgnoreCase(request.username())) {
			throw new DuplicateResourceException("username",
					"Handle @%s is already taken.".formatted(request.username()));
		}

		me.setUsername(request.username());
		return CurrentUserResponse.from(me);
	}
}
