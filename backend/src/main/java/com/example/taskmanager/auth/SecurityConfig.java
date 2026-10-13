package com.example.taskmanager.auth;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Session-cookie authentication for the single-page app.
 *
 * <ul>
 *   <li>The session id lives in an HttpOnly cookie, so JavaScript never sees a credential.</li>
 *   <li>CSRF protection uses the SPA pattern: the token is sent in a readable {@code XSRF-TOKEN}
 *       cookie and must be echoed back in the {@code X-XSRF-TOKEN} header on mutating requests.</li>
 *   <li>Unauthenticated API calls get a plain {@code 401} instead of a redirect to a login page.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(LoginAttemptProperties.class)
public class SecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurity(HttpSecurity http, SecurityContextRepository securityContextRepository)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.spa())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
        return http.build();
    }

    /** The H2 web console (dev only) has its own login, runs in frames and posts forms without our CSRF header. */
    @Bean
    @Order(1)
    @ConditionalOnBooleanProperty("spring.h2.console.enabled")
    SecurityFilterChain h2ConsoleSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Stores hashes as "{bcrypt}..." so the algorithm can be upgraded later without a migration.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
