package com.threadly.post;

import com.threadly.common.domain.Auditable;
import com.threadly.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single post.
 *
 * <p>Deletion is soft: replies and reposts will point at this row, and removing it outright would
 * leave them dangling. A deleted post keeps its id but stops being served.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends Auditable {

	public static final int MAX_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = MAX_LENGTH)
	private String content;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	/** Set the first time the text is changed; drives the "edited" marker in clients. */
	@Column(name = "edited_at")
	private Instant editedAt;

	private Post(User author, String content) {
		this.author = author;
		this.content = content;
	}

	public static Post write(User author, String content) {
		return new Post(author, content.strip());
	}

	public void edit(String content) {
		this.content = content.strip();
		this.editedAt = Instant.now();
	}

	public void delete() {
		if (deletedAt == null) {
			deletedAt = Instant.now();
		}
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isAuthoredBy(User user) {
		return author.getId().equals(user.getId());
	}
}
