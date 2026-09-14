package com.threadly.follow;

import com.threadly.common.domain.Auditable;
import com.threadly.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One edge of the follow graph.
 *
 * <p>Modelled as an entity rather than a {@code @ManyToMany} collection: the edge carries its own
 * data — when it was created, and later who notified whom — and it has to be paginated and counted
 * without loading either side's full list.
 */
@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "follower_id", nullable = false)
	private User follower;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "followee_id", nullable = false)
	private User followee;

	private Follow(User follower, User followee) {
		this.follower = follower;
		this.followee = followee;
	}

	public static Follow of(User follower, User followee) {
		return new Follow(follower, followee);
	}
}
