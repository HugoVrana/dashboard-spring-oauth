package com.dashboard.oauth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        boolean hasUppercase = password.matches(UPPERCASE_PATTERN);
        boolean hasLowercase = password.matches(LOWERCASE_PATTERN);
        boolean hasDigit = password.matches(DIGIT_PATTERN);
        boolean hasSpecialChar = password.matches(SPECIAL_CHAR_PATTERN);

        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
}
