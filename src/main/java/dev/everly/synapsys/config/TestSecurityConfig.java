package dev.everly.synapsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("test")
public class TestSecurityConfig {

	@Bean
	public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(c -> c.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/api/health", "/api/**").permitAll()
						.anyRequest().denyAll())
				.build();
	}
}
