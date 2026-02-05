package dev.everly.synapsys.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.everly.synapsys.authentication.CachedBodyFilter;

@Configuration
public class FilterRegistrationConfig {

	@Bean
	public FilterRegistrationBean<ApiKeyAuthFilter> disableApiKeyAuthFilterRegistration(ApiKeyAuthFilter filter) {
		FilterRegistrationBean<ApiKeyAuthFilter> reg = new FilterRegistrationBean<>(filter);
		reg.setEnabled(false);
		return reg;
	}

	@Bean
	public FilterRegistrationBean<CachedBodyFilter> disableCachedBodyFilterRegistration(CachedBodyFilter filter) {
		FilterRegistrationBean<CachedBodyFilter> reg = new FilterRegistrationBean<>(filter);
		reg.setEnabled(false);
		return reg;
	}
}
