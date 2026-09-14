package com.threadly.notification;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	long countByRecipientIdAndReadAtIsNull(Long recipientId);

	@Query("""
			select n from Notification n
			join fetch n.actor
			left join fetch n.post
			where n.recipient.id = :recipientId
			order by n.createdAt desc, n.id desc
			""")
	List<Notification> findInbox(@Param("recipientId") Long recipientId, Limit limit);

	@Query("""
			select n from Notification n
			join fetch n.actor
			left join fetch n.post
			where n.recipient.id = :recipientId
			  and (n.createdAt < :createdAt or (n.createdAt = :createdAt and n.id < :id))
			order by n.createdAt desc, n.id desc
			""")
	List<Notification> findInboxBefore(
			@Param("recipientId") Long recipientId,
			@Param("createdAt") Instant createdAt,
			@Param("id") Long id,
			Limit limit);

	@Modifying
	@Query("""
			update Notification n set n.readAt = :now
			where n.recipient.id = :recipientId and n.readAt is null
			""")
	int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);
}
