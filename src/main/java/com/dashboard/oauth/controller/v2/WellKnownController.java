package com.dashboard.oauth.controller.v2;

import com.dashboard.oauth.config.RsaKeyPair;
import com.dashboard.oauth.environment.OidcProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "OIDC Discovery", description = "OpenID Connect discovery and JWKS endpoints")
public class WellKnownController {

    private final RsaKeyPair rsaKeyPair;
    private final OidcProperties oidcProperties;

    @Operation(summary = "OpenID Connect Discovery (RFC 5785)")
    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openidConfiguration() {
        String issuer = oidcProperties.getIssuer();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("issuer", issuer);
        config.put("authorization_endpoint", issuer + "/v2/oauth2/authorize");
        config.put("token_endpoint", issuer + "/v2/oauth2/token");
        config.put("jwks_uri", issuer + "/.well-known/jwks.json");
        config.put("response_types_supported", List.of("code"));
        config.put("subject_types_supported", List.of("public"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));
        config.put("scopes_supported", List.of("openid", "profile", "email"));
        config.put("token_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post", "none"));
        config.put("claims_supported",
                List.of("sub", "iss", "aud", "exp", "iat", "email", "nonce"));
        config.put("code_challenge_methods_supported", List.of("S256"));
        return config;
    }

    @Operation(summary = "JSON Web Key Set (RFC 7517)")
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = rsaKeyPair.getPublicKey();
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", rsaKeyPair.getKid());
        jwk.put("n", base64UrlEncode(publicKey.getModulus()));
        jwk.put("e", base64UrlEncode(publicKey.getPublicExponent()));
        return Map.of("keys", List.of(jwk));
    }

    private static String base64UrlEncode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
