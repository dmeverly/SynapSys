package dev.everly.synapsys.authentication;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachedBodyFilter extends OncePerRequestFilter {

	private static final int MAX_BODY_BYTES = 8 * 1024;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, MAX_BODY_BYTES);
		filterChain.doFilter(wrapped, response);
	}
}
