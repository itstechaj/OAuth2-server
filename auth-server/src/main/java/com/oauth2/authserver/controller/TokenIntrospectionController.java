package com.oauth2.authserver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/introspect")
public class TokenIntrospectionController {

    private final JwtDecoder jwtDecoder;

    public TokenIntrospectionController(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping
    public Map<String, Object> introspect(@RequestParam("token") String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            Jwt jwt = jwtDecoder.decode(token);

            response.put("active", true);
            response.put("sub", jwt.getSubject());
            response.put("client_id", jwt.getClaimAsString("client_id"));
            response.put("scopes", jwt.getClaimAsStringList("scope"));
            response.put("iss", jwt.getIssuer().toString());
            response.put("iat", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : null);
            response.put("exp", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null);
            response.put("token_type", "Bearer");

            // Include all claims
            Map<String, Object> allClaims = new HashMap<>(jwt.getClaims());
            allClaims.remove("iat");
            allClaims.remove("exp");
            allClaims.remove("sub");
            allClaims.remove("iss");
            allClaims.remove("scope");
            allClaims.remove("client_id");
            if (!allClaims.isEmpty()) {
                response.put("additional_claims", allClaims);
            }

        } catch (JwtException e) {
            response.put("active", false);
            response.put("error", "Invalid or expired token");
            response.put("error_description", e.getMessage());
        }

        return response;
    }
}
