package com.threadly.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/**
 * Checks a password against a handful of rules that between them rule out the passwords that
 * actually get broken.
 *
 * <p>Length does most of the work, so the floor is higher than the eight characters a password
 * field conventionally asks for. The character-class rules stop the obvious single-class choices.
 * The blocklist catches the rest: {@code Password123!} satisfies every structural rule and is
 * still among the first things any attacker tries.
 *
 * <p>Deliberately not here: forced rotation, and a ban on repeated or sequential characters.
 * Both push people toward writing passwords down, and current NIST guidance advises against them.
 */
@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

	static final int MIN_LENGTH = 10;
	/** BCrypt ignores anything past 72 bytes, so accepting more would silently do nothing. */
	static final int MAX_LENGTH = 72;

	private static final Set<String> COMMON_PASSWORDS = loadCommonPasswords();

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		// Absence is @NotBlank's business, not this constraint's.
		if (password == null || password.isEmpty()) {
			return true;
		}

		List<String> problems = problemsWith(password);
		if (problems.isEmpty()) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate("must " + String.join(", ", problems))
				.addConstraintViolation();
		return false;
	}

	/** Every unmet rule, phrased to read after the word "must". */
	static List<String> problemsWith(String password) {
		List<String> problems = new ArrayList<>();

		if (password.length() < MIN_LENGTH) {
			problems.add("be at least " + MIN_LENGTH + " characters");
		}
		if (password.length() > MAX_LENGTH) {
			problems.add("be at most " + MAX_LENGTH + " characters");
		}
		if (password.chars().noneMatch(Character::isLowerCase)) {
			problems.add("contain a lowercase letter");
		}
		if (password.chars().noneMatch(Character::isUpperCase)) {
			problems.add("contain an uppercase letter");
		}
		if (password.chars().noneMatch(Character::isDigit)) {
			problems.add("contain a digit");
		}
		if (password.chars().noneMatch(StrongPasswordValidator::isSpecial)) {
			problems.add("contain a symbol such as _ ! ? or -");
		}
		if (password.chars().anyMatch(Character::isWhitespace)) {
			problems.add("not contain spaces");
		}
		if (isCommon(password)) {
			problems.add("not be a commonly used password");
		}

		return problems;
	}

	private static boolean isSpecial(int codePoint) {
		return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
	}

	/**
	 * Compared in lower case and with trailing digits removed, because {@code Password1} and
	 * {@code password} are the same guess to anyone running a dictionary.
	 */
	private static boolean isCommon(String password) {
		String normalised = password.toLowerCase(Locale.ROOT);
		return COMMON_PASSWORDS.contains(normalised)
				|| COMMON_PASSWORDS.contains(normalised.replaceAll("[^a-z]+$", ""));
	}

	private static Set<String> loadCommonPasswords() {
		Set<String> loaded = new HashSet<>();
		ClassPathResource resource = new ClassPathResource("security/common-passwords.txt");

		try (InputStream stream = resource.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String entry = line.strip().toLowerCase(Locale.ROOT);
				if (!entry.isEmpty() && !entry.startsWith("#")) {
					loaded.add(entry);
				}
			}
		}
		catch (IOException e) {
			// A missing list must not make every password invalid, nor every password valid by
			// silence: the structural rules still apply, and the gap is loud in the log.
			log.error("Could not load the common-password list; that check is disabled", e);
		}

		return Set.copyOf(loaded);
	}
}
