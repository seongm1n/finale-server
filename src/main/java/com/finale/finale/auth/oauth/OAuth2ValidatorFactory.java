package com.finale.finale.auth.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2ValidatorFactory {

    private final List<OAuth2TokenValidator> validators;
    private Map<String, OAuth2TokenValidator> validatorMap;

    private void initValidatorMap() {
        if (validatorMap == null) {
            validatorMap = new HashMap<>();
            for (OAuth2TokenValidator validator : validators) {
                validatorMap.put(validator.getProviderName(), validator);
            }
        }
    }

    public OAuth2TokenValidator getValidator(String provider) {
        initValidatorMap();

        OAuth2TokenValidator validator = validatorMap.get(provider.toLowerCase());
        if (validator == null) {
            throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        }

        return validator;
    }
}
