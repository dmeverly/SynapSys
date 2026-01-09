package dev.everly.synapsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthFilter authFilter) throws Exception {
		return http.csrf(c -> c.disable())
				.authorizeHttpRequests(auth -> auth

						.requestMatchers("/actuator/health", "/api/health", "/health").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class).build();
	}
}
