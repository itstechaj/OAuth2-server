package com.oauth2.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Configuration
public class DefaultSecurityConfig {

    @Value("${login-server.url}")
    private String loginServerUrl;

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/clients", "/api/introspect", "/login", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/clients", "/api/introspect", "/login")
                )
                .authenticationProvider(externalAuthenticationProvider());

        return http.build();
    }

    @Bean
    public AuthenticationProvider externalAuthenticationProvider() {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();

                // Validate credentials against login server
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    Map<String, String> request = Map.of("username", username, "password", password);
                    restTemplate.postForEntity(
                            loginServerUrl + "/api/authenticate",
                            request,
                            String.class
                    );
                    // If we get here, authentication was successful
                    return new UsernamePasswordAuthenticationToken(
                            username, password,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                } catch (Exception e) {
                    throw new BadCredentialsException("Invalid username or password");
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
