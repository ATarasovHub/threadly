package com.threadly.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("select p from Post p join fetch p.author where p.id = :id and p.deletedAt is null")
	Optional<Post> findVisibleById(@Param("id") Long id);

	@Query("select count(p) from Post p where p.author.id = :authorId and p.deletedAt is null")
	long countVisibleByAuthorId(@Param("authorId") Long authorId);

	/**
	 * The Following feed: posts by accounts the viewer follows, plus the viewer's own, newest
	 * first.
	 *
	 * <p>Written as an {@code exists} subquery rather than a join so a post cannot appear twice,
	 * and so the planner can use the {@code (follower_id, ...)} index on follows directly.
	 */
	@Query("""
			select p from Post p
			join fetch p.author a
			where p.deletedAt is null
			  and (a.id = :viewerId
			       or exists (select 1 from Follow f
			                  where f.follower.id = :viewerId and f.followee.id = a.id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findFollowingFeed(@Param("viewerId") Long viewerId, Limit limit);

	@Query("""
			select p from Post p
			join fetch p.author a
			where p.deletedAt is null
			  and (a.id = :viewerId
			       or exists (select 1 from Follow f
			                  where f.follower.id = :viewerId and f.followee.id = a.id))
			  and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findFollowingFeedBefore(
			@Param("viewerId") Long viewerId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	/**
	 * The For You feed. For now this is every visible post, newest first: with no engagement
	 * signals yet, recency is the only honest ranking. Ranking belongs here when likes exist.
	 */
	@Query("""
			select p from Post p
			join fetch p.author
			where p.deletedAt is null
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findGlobalFeed(Limit limit);

	@Query("""
			select p from Post p
			join fetch p.author
			where p.deletedAt is null
			  and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findGlobalFeedBefore(
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	/** First page of an author's timeline, newest first. */
	@Query("""
			select p from Post p
			join fetch p.author a
			where a.id = :authorId
			  and p.deletedAt is null
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findAuthorTimeline(@Param("authorId") Long authorId, Limit limit);

	/**
	 * The page following a cursor.
	 *
	 * <p>Kept separate from the first-page query rather than folded in behind a null check:
	 * PostgreSQL cannot infer the type of an untyped null parameter, and two narrow queries also
	 * match the {@code (author_id, created_at desc, id desc)} index more directly.
	 *
	 * <p>The comparison is on the pair {@code (createdAt, id)}, so posts written within the same
	 * timestamp are still walked exactly once.
	 */
	@Query("""
			select p from Post p
			join fetch p.author a
			where a.id = :authorId
			  and p.deletedAt is null
			  and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findAuthorTimelineBefore(
			@Param("authorId") Long authorId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);
}
