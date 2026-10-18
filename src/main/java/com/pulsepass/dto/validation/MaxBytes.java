package com.pulsepass.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El texto, codificado en UTF-8, no puede superar {@link #value()} bytes.
 * BCrypt solo procesa los primeros 72 bytes: @Size cuenta caracteres, no bytes.
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    String message() default "Value is too long";

    int value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
