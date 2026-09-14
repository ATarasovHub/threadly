package com.threadly.bookmark;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	boolean existsByPostIdAndUserId(Long postId, Long userId);

	long deleteByPostIdAndUserId(Long postId, Long userId);

	/** Which of these posts the viewer has saved — one query for a whole page. */
	@Query("select b.post.id from Bookmark b where b.user.id = :userId and b.post.id in :postIds")
	List<Long> findBookmarkedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

	/**
	 * The saved list, ordered by when the post was saved rather than when it was written, which
	 * is why it pages on the bookmark's own timestamp.
	 */
	@Query("""
			select b from Bookmark b
			join fetch b.post p
			join fetch p.author
			left join fetch p.repostOf o
			left join fetch o.author
			where b.user.id = :userId and p.deletedAt is null
			order by b.createdAt desc, b.id desc
			""")
	List<Bookmark> findSaved(@Param("userId") Long userId, Limit limit);

	@Query("""
			select b from Bookmark b
			join fetch b.post p
			join fetch p.author
			left join fetch p.repostOf o
			left join fetch o.author
			where b.user.id = :userId and p.deletedAt is null
			  and (b.createdAt < :createdAt or (b.createdAt = :createdAt and b.id < :id))
			order by b.createdAt desc, b.id desc
			""")
	List<Bookmark> findSavedBefore(
			@Param("userId") Long userId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);
}
