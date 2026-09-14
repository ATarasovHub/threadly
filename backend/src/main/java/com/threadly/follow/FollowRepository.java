package com.threadly.follow;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

	boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

	long deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

	long countByFolloweeId(Long followeeId);

	long countByFollowerId(Long followerId);

	/** Accounts following {@code followeeId}, most recent first. */
	@Query("""
			select f from Follow f
			join fetch f.follower
			where f.followee.id = :followeeId
			order by f.createdAt desc, f.id desc
			""")
	List<Follow> findFollowers(@Param("followeeId") Long followeeId, Limit limit);

	@Query("""
			select f from Follow f
			join fetch f.follower
			where f.followee.id = :followeeId
			  and (f.createdAt < :createdAt or (f.createdAt = :createdAt and f.id < :id))
			order by f.createdAt desc, f.id desc
			""")
	List<Follow> findFollowersBefore(
			@Param("followeeId") Long followeeId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	/** Accounts that {@code followerId} follows, most recent first. */
	@Query("""
			select f from Follow f
			join fetch f.followee
			where f.follower.id = :followerId
			order by f.createdAt desc, f.id desc
			""")
	List<Follow> findFollowing(@Param("followerId") Long followerId, Limit limit);

	@Query("""
			select f from Follow f
			join fetch f.followee
			where f.follower.id = :followerId
			  and (f.createdAt < :createdAt or (f.createdAt = :createdAt and f.id < :id))
			order by f.createdAt desc, f.id desc
			""")
	List<Follow> findFollowingBefore(
			@Param("followerId") Long followerId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);
}
