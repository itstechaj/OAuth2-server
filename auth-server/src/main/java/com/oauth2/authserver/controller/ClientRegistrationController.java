package com.oauth2.authserver.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/clients")
public class ClientRegistrationController {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientRegistrationController(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerClient(@RequestBody ClientRegistrationRequest request) {
        // Check if client already exists
        if (registeredClientRepository.findByClientId(request.clientId()) != null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Client with id '" + request.clientId() + "' already exists"));
        }

        // Generate a secure client secret
        String rawSecret = UUID.randomUUID().toString();

        // Build the registered client
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.clientId())
                .clientSecret(passwordEncoder.encode(rawSecret))
                .clientName(request.clientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);

        // Add grant types
        for (String grantType : request.grantTypes()) {
            switch (grantType.toLowerCase()) {
                case "authorization_code":
                    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                    builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                    break;
                case "client_credentials":
                    builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Unsupported grant type: " + grantType));
            }
        }

        // Add redirect URI
        if (request.redirectUri() != null && !request.redirectUri().isEmpty()) {
            builder.redirectUri(request.redirectUri());
        }

        // Add scopes
        for (String scope : request.scopes()) {
            builder.scope(scope);
        }

        // Configure client settings
        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .build());

        // Configure token settings
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                .build());

        RegisteredClient registeredClient = builder.build();
        registeredClientRepository.save(registeredClient);

        return ResponseEntity.ok(Map.of(
                "client_id", request.clientId(),
                "client_secret", rawSecret,
                "scopes", request.scopes(),
                "grant_types", request.grantTypes(),
                "redirect_uri", request.redirectUri() != null ? request.redirectUri() : "",
                "message", "Client registered successfully. Save the client_secret — it cannot be retrieved again."));
    }

    public record ClientRegistrationRequest(
            String clientId,
            List<String> scopes,
            List<String> grantTypes,
            String redirectUri) {
    }
}
