package dev.everly.synapsys.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			String traceId = UUID.randomUUID().toString().substring(0, 8);
			MDC.put("traceId", traceId);
			chain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
