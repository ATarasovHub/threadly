package com.threadly.like;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

	boolean existsByPostIdAndUserId(Long postId, Long userId);

	long deleteByPostIdAndUserId(Long postId, Long userId);

	/**
	 * Like counts for a whole page of posts in one query.
	 *
	 * <p>Counting per post while rendering a feed would issue one query per row — the N+1 problem
	 * that makes timelines slow as soon as they are longer than a screen.
	 */
	@Query("""
			select l.post.id, count(l) from PostLike l
			where l.post.id in :postIds
			group by l.post.id
			""")
	List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

	/** Which of these posts the viewer has already liked, again in one query. */
	@Query("select l.post.id from PostLike l where l.user.id = :userId and l.post.id in :postIds")
	List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
