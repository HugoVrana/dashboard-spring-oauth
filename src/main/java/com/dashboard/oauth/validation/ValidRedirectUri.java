package com.dashboard.oauth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidRedirectUriValidator.class)
@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRedirectUri {
    String message() default "Redirect URI must be an absolute http or https URI with no fragment component";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
