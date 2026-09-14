package com.threadly.block;

import com.threadly.common.error.BadRequestException;
import com.threadly.common.error.ResourceNotFoundException;
import com.threadly.follow.FollowRepository;
import com.threadly.user.CurrentUserService;
import com.threadly.user.User;
import com.threadly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

	private final BlockRepository blocks;
	private final FollowRepository follows;
	private final UserRepository users;
	private final CurrentUserService currentUserService;

	/**
	 * Blocks an account.
	 *
	 * <p>Any follow edges between the two are torn down in both directions. Leaving them in place
	 * would keep the blocked account in follower counts and would put its posts back in the feed
	 * the moment the block is lifted, which is not what "block" means to anyone.
	 */
	@Transactional
	public void block(String username) {
		User me = currentUserService.require();
		User target = requireAccount(username);

		if (target.getId().equals(me.getId())) {
			throw new BadRequestException("You cannot block yourself.");
		}
		if (blocks.existsByBlockerIdAndBlockedId(me.getId(), target.getId())) {
			return;
		}

		follows.deleteByFollowerIdAndFolloweeId(me.getId(), target.getId());
		follows.deleteByFollowerIdAndFolloweeId(target.getId(), me.getId());

		try {
			blocks.save(Block.of(me, target));
		}
		catch (DataIntegrityViolationException e) {
			// Concurrent duplicate; the account is blocked either way.
		}
	}

	/** Lifting a block does not restore the follows it removed; that is the caller's to redo. */
	@Transactional
	public void unblock(String username) {
		User me = currentUserService.require();
		User target = requireAccount(username);
		blocks.deleteByBlockerIdAndBlockedId(me.getId(), target.getId());
	}

	@Transactional(readOnly = true)
	public boolean isBlockedBetween(Long first, Long second) {
		return blocks.existsBetween(first, second);
	}

	private User requireAccount(String username) {
		return users.findByUsernameIgnoreCase(username)
				.filter(User::isEnabled)
				.orElseThrow(() -> new ResourceNotFoundException("No account with handle @" + username));
	}
}
