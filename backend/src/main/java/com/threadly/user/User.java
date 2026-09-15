package com.threadly.user;

import com.threadly.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Threadly account.
 *
 * <p>{@code username} is the public handle used in URLs such as {@code /andrii}. It is stored with
 * the casing the user chose but is unique case-insensitively, enforced by a functional index in
 * migration {@code V1}.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	@Setter
	private String username;

	@Column(nullable = false, length = 254)
	@Setter
	private String email;

	/**
	 * BCrypt hash; never leaves the persistence layer.
	 *
	 * <p>Null for an account created through an external provider, which has no password to hash.
	 */
	@Column(name = "password_hash", length = 100)
	@Setter
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 50)
	@Setter
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(length = 160)
	private String bio;

	@Column(length = 50)
	private String location;

	/** Absolute http(s) URL; the scheme is enforced by a CHECK constraint in migration V3. */
	@Column(length = 200)
	private String website;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Column(name = "banner_url", length = 500)
	private String bannerUrl;

	/** Disabled accounts keep their data but cannot authenticate. */
	@Column(nullable = false)
	@Setter
	private boolean enabled;

	@Builder
	private User(String username, String email, String passwordHash, String displayName, Role role, boolean enabled) {
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role == null ? Role.USER : role;
		this.enabled = enabled;
	}

	public boolean hasPassword() {
		return passwordHash != null;
	}

	/**
	 * Applies a profile edit. A {@code null} argument leaves the field untouched; a blank one
	 * clears it, which is how the API expresses "remove my bio".
	 */
	public void updateProfile(String displayName, String bio, String location, String website,
			String avatarUrl, String bannerUrl) {
		if (displayName != null) {
			this.displayName = displayName.trim();
		}
		this.bio = replaceIfPresent(this.bio, bio);
		this.location = replaceIfPresent(this.location, location);
		this.website = replaceIfPresent(this.website, website);
		this.avatarUrl = replaceIfPresent(this.avatarUrl, avatarUrl);
		this.bannerUrl = replaceIfPresent(this.bannerUrl, bannerUrl);
	}

	private static String replaceIfPresent(String current, String incoming) {
		if (incoming == null) {
			return current;
		}
		String trimmed = incoming.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
