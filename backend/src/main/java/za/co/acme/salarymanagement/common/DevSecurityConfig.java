package za.co.acme.salarymanagement.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY development-only security configuration.
 *
 * <p>Purpose: let Postman / browser-based API testing reach the employee
 * endpoints while the real JWT configuration is still being built. CSRF is
 * disabled for API calls and sessions are stateless; requests are permitted
 * without authentication.
 *
 * <p>This class is deleted when the security milestone is implemented. It is
 * guarded by the "dev" profile so it must never be active in production.
 *
 * <p>To activate it: start the application with the "dev" profile
 * (-Dspring.profiles.active=dev, or SPRING_PROFILES_ACTIVE=dev, or
 * spring.profiles.active=dev in application.properties for now).
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}