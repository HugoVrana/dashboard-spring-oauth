package com.dashboard.oauth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

public class ValidRedirectUriValidator implements ConstraintValidator<ValidRedirectUri, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                return false;
            }
            if (uri.getFragment() != null) {
                return false;
            }
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
