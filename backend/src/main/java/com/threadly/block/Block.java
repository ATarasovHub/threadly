package com.threadly.block;

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

@Entity
@Table(name = "blocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blocker_id", nullable = false)
	private User blocker;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blocked_id", nullable = false)
	private User blocked;

	private Block(User blocker, User blocked) {
		this.blocker = blocker;
		this.blocked = blocked;
	}

	public static Block of(User blocker, User blocked) {
		return new Block(blocker, blocked);
	}
}
