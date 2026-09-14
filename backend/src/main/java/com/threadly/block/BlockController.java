package com.threadly.block;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{username}")
@RequiredArgsConstructor
public class BlockController {

	private final BlockService blockService;

	@PostMapping("/block")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void block(@PathVariable String username) {
		blockService.block(username);
	}

	@DeleteMapping("/block")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unblock(@PathVariable String username) {
		blockService.unblock(username);
	}
}
