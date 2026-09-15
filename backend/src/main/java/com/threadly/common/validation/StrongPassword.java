package com.threadly.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires a password that is not trivially guessable.
 *
 * <p>The validator reports every unmet rule in one message rather than the first, so a caller
 * fixes the password once instead of discovering the requirements one rejection at a time.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

	String message() default "is not strong enough";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
