package com.threadly.post;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("""
			select p from Post p
			join fetch p.author
			left join fetch p.repostOf o
			left join fetch o.author
			where p.id = :id and p.deletedAt is null
			""")
	Optional<Post> findVisibleById(@Param("id") Long id);

	boolean existsByAuthorIdAndRepostOfIdAndContentIsNullAndDeletedAtIsNull(Long authorId, Long repostOfId);

	long deleteByAuthorIdAndRepostOfIdAndContentIsNull(Long authorId, Long repostOfId);

	/** Repost counts for a page of posts; quote posts count too. */
	@Query("""
			select p.repostOf.id, count(p) from Post p
			where p.repostOf.id in :originalIds and p.deletedAt is null
			group by p.repostOf.id
			""")
	List<Object[]> countRepostsByOriginalIds(@Param("originalIds") Collection<Long> originalIds);

	/** Which of these posts the viewer has plainly reposted. */
	@Query("""
			select p.repostOf.id from Post p
			where p.author.id = :viewerId and p.content is null and p.deletedAt is null
			  and p.repostOf.id in :originalIds
			""")
	List<Long> findRepostedIds(
			@Param("viewerId") Long viewerId, @Param("originalIds") Collection<Long> originalIds);

	@Query("""
			select count(p) from Post p
			where p.author.id = :authorId and p.deletedAt is null and p.parent is null
			""")
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
			left join fetch p.repostOf ro
			left join fetch ro.author
			where p.deletedAt is null
			  and p.parent is null
			  and (a.id = :viewerId
			       or exists (select 1 from Follow f
			                  where f.follower.id = :viewerId and f.followee.id = a.id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findFollowingFeed(@Param("viewerId") Long viewerId, Limit limit);

	@Query("""
			select p from Post p
			join fetch p.author a
			left join fetch p.repostOf ro
			left join fetch ro.author
			where p.deletedAt is null
			  and p.parent is null
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
			join fetch p.author a
			left join fetch p.repostOf ro
			left join fetch ro.author
			where p.deletedAt is null
			  and p.parent is null
			  and not exists (select 1 from Block b
			                  where (b.blocker.id = :viewerId and b.blocked.id = a.id)
			                     or (b.blocker.id = a.id and b.blocked.id = :viewerId))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findGlobalFeed(@Param("viewerId") Long viewerId, Limit limit);

	@Query("""
			select p from Post p
			join fetch p.author a
			left join fetch p.repostOf ro
			left join fetch ro.author
			where p.deletedAt is null
			  and p.parent is null
			  and not exists (select 1 from Block b
			                  where (b.blocker.id = :viewerId and b.blocked.id = a.id)
			                     or (b.blocker.id = a.id and b.blocked.id = :viewerId))
			  and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findGlobalFeedBefore(
			@Param("viewerId") Long viewerId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	/**
	 * Replies to a post, oldest first.
	 *
	 * <p>The only listing in the API that runs forwards in time: a conversation is read from the
	 * beginning, not from its latest message. The cursor comparison is flipped to match.
	 */
	@Query("""
			select p from Post p
			join fetch p.author
			left join fetch p.repostOf ro2
			left join fetch ro2.author
			where p.parent.id = :parentId and p.deletedAt is null
			order by p.createdAt asc, p.id asc
			""")
	List<Post> findReplies(@Param("parentId") Long parentId, Limit limit);

	@Query("""
			select p from Post p
			join fetch p.author
			left join fetch p.repostOf ro2
			left join fetch ro2.author
			where p.parent.id = :parentId and p.deletedAt is null
			  and (p.createdAt > :createdAt or (p.createdAt = :createdAt and p.id > :id))
			order by p.createdAt asc, p.id asc
			""")
	List<Post> findRepliesAfter(
			@Param("parentId") Long parentId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	/** Reply counts for a whole page of posts, in one query. */
	@Query("""
			select p.parent.id, count(p) from Post p
			where p.parent.id in :parentIds and p.deletedAt is null
			group by p.parent.id
			""")
	List<Object[]> countRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

	/** First page of an author's timeline, newest first. */
	@Query("""
			select p from Post p
			join fetch p.author a
			left join fetch p.repostOf ro
			left join fetch ro.author
			where a.id = :authorId
			  and p.deletedAt is null
			  and p.parent is null
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
			left join fetch p.repostOf ro
			left join fetch ro.author
			where a.id = :authorId
			  and p.deletedAt is null
			  and p.parent is null
			  and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findAuthorTimelineBefore(
			@Param("authorId") Long authorId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);
}
