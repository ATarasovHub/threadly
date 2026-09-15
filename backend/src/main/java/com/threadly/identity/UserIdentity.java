package com.threadly.identity;

import com.threadly.common.domain.Auditable;
import com.threadly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** A link between a Threadly account and an account at an external provider. */
@Entity
@Table(name = "user_identities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IdentityProvider provider;

	/** The provider's stable subject id. Never the email, which can change hands. */
	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	@Column(length = 254)
	private String email;

	private UserIdentity(User user, IdentityProvider provider, String providerUserId, String email) {
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.email = email;
	}

	public static UserIdentity of(User user, IdentityProvider provider, String providerUserId, String email) {
		return new UserIdentity(user, provider, providerUserId, email);
	}
}
