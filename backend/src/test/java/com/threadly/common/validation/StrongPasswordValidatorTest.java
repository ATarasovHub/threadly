package com.threadly.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Plain unit tests: the rules are pure logic and deserve to be checked without a Spring context. */
class StrongPasswordValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"Thread_ly2026",
			"correct-Horse-Battery-9",
			"n0t+Very|Obvious",
			"Aa1_aaaaaa",
	})
	void acceptsPasswordsThatMeetEveryRule(String password) {
		assertThat(StrongPasswordValidator.problemsWith(password)).isEmpty();
	}

	@Test
	void rejectsSomethingTooShort() {
		assertThat(StrongPasswordValidator.problemsWith("Aa1_aaa"))
				.contains("be at least 10 characters");
	}

	@Test
	void rejectsSomethingLongerThanBcryptWouldRead() {
		// Anything past 72 bytes is ignored by BCrypt, so accepting it would be a false promise.
		assertThat(StrongPasswordValidator.problemsWith("Aa1_" + "x".repeat(80)))
				.contains("be at most 72 characters");
	}

	@Test
	void namesEveryMissingCharacterClassAtOnce() {
		// One message listing all of it beats three rejections in a row.
		List<String> problems = StrongPasswordValidator.problemsWith("aaaaaaaaaaaa");

		assertThat(problems).containsExactlyInAnyOrder(
				"contain an uppercase letter",
				"contain a digit",
				"contain a symbol such as _ ! ? or -");
	}

	@Test
	void requiresASymbol() {
		assertThat(StrongPasswordValidator.problemsWith("Threadly2026"))
				.containsExactly("contain a symbol such as _ ! ? or -");
	}

	@Test
	void rejectsSpaces() {
		assertThat(StrongPasswordValidator.problemsWith("Thread ly_2026"))
				.contains("not contain spaces");
	}

	@Test
	void rejectsACommonPasswordEvenWhenItSatisfiesEveryStructuralRule() {
		// Structurally this passes everything; it is also one of the first guesses anyone makes.
		assertThat(StrongPasswordValidator.problemsWith("Password123!"))
				.containsExactly("not be a commonly used password");
	}

	@Test
	void seesThroughTrailingDigitsOnACommonPassword() {
		assertThat(StrongPasswordValidator.problemsWith("Qwerty_2026"))
				.containsExactly("not be a commonly used password");
	}

	@Test
	void leavesAnEmptyValueToNotBlank() {
		// Checked through the real constraint, since the short-circuit lives in isValid: two
		// constraints reporting the same missing field would be noise.
		assertThat(violationsFor("")).isEmpty();
	}

	@Test
	void reportsTheUnmetRulesAsASingleMessage() {
		assertThat(violationsFor("aaaaaaaaaaaa"))
				.singleElement()
				.satisfies(message -> assertThat(message)
						.startsWith("must ")
						.contains("uppercase", "digit", "symbol"));
	}

	private static List<String> violationsFor(String password) {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			return factory.getValidator()
					.validate(new Holder(password))
					.stream()
					.map(ConstraintViolation::getMessage)
					.toList();
		}
	}

	private record Holder(@StrongPassword String password) {
	}
}
