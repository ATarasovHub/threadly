package com.threadly.block;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, Long> {

	boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

	long deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

	/**
	 * Whether a block exists in either direction.
	 *
	 * <p>Visibility is symmetric on purpose: if it were not, the blocked account could still read
	 * everything the blocker writes, which is the main thing blocking is meant to stop.
	 */
	@Query("""
			select count(b) > 0 from Block b
			where (b.blocker.id = :first and b.blocked.id = :second)
			   or (b.blocker.id = :second and b.blocked.id = :first)
			""")
	boolean existsBetween(@Param("first") Long first, @Param("second") Long second);
}
