package com.threadly.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.threadly.TestcontainersConfiguration;
import com.threadly.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Runs against a real PostgreSQL container with the Flyway migrations applied, so it also proves
 * that the entity mapping and the schema agree (JPA runs with ddl-auto=validate).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class})
class UserRepositoryTest {

	@Autowired
	private UserRepository users;

	private static User.UserBuilder user(String username, String email) {
		return User.builder()
				.username(username)
				.email(email)
				.passwordHash("$2a$10$notarealhashnotarealhashnotarealhashnotarealhashnotare")
				.displayName(username)
				.role(Role.USER)
				.enabled(true);
	}

	@Test
	void persistsAccountAndFillsAuditingTimestamps() {
		User saved = users.saveAndFlush(user("andrii", "andrii@example.com").build());

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getRole()).isEqualTo(Role.USER);
	}

	@Test
	void looksUpAccountIgnoringHandleCase() {
		users.saveAndFlush(user("Andrii", "andrii@example.com").build());

		assertThat(users.findByUsernameIgnoreCase("ANDRII")).isPresent();
		assertThat(users.existsByUsernameIgnoreCase("andrii")).isTrue();
		assertThat(users.existsByEmailIgnoreCase("ANDRII@example.com")).isTrue();
	}

	@Test
	void rejectsHandleThatDiffersOnlyByCase() {
		users.saveAndFlush(user("andrii", "andrii@example.com").build());

		assertThatThrownBy(() -> users.saveAndFlush(user("ANDRII", "other@example.com").build()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateEmail() {
		users.saveAndFlush(user("andrii", "andrii@example.com").build());

		assertThatThrownBy(() -> users.saveAndFlush(user("anna", "ANDRII@example.com").build()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsHandleWithIllegalCharacters() {
		// IDENTITY generation makes Hibernate issue the INSERT during save(), so the CHECK
		// constraint fires here rather than on flush.
		assertThatThrownBy(() -> users.save(user("bad handle!", "bad@example.com").build()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
