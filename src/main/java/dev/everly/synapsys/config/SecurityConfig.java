package dev.everly.synapsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import dev.everly.synapsys.authentication.CachedBodyFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CachedBodyFilter cachedBodyFilter,
			ApiKeyAuthFilter authFilter) throws Exception {

		return http.csrf(c -> c.disable())
				.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/api/health", "/health")
						.permitAll().requestMatchers("/api/**").authenticated().anyRequest().denyAll())
				.addFilterBefore(cachedBodyFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(authFilter, CachedBodyFilter.class).build();
	}
}
