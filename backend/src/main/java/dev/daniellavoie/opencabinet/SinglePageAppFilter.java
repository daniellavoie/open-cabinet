package dev.daniellavoie.opencabinet;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Forwards browser navigations to Angular routes (e.g. /projects/42) to index.html so the
 * single-page app can resolve them.
 */
@Component
public class SinglePageAppFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest) {
			String path = httpRequest.getRequestURI();
			String accept = httpRequest.getHeader("Accept");
			if (!path.startsWith("/api/") && !path.startsWith("/actuator")
					&& accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
				request.getRequestDispatcher("/index.html").forward(request, response);
				return;
			}
		}

		chain.doFilter(request, response);
	}
}
