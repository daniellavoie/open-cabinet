package dev.daniellavoie.opencabinet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the Angular app, which is built once per language under /fr/ and /en/.
 *
 * <p>
 * Browser navigations inside a language (e.g. /en/projects/42) get that language's index.html so
 * the single-page app can resolve the route. Navigations outside one (/, or links like /projects)
 * are redirected into the visitor's language: the one they chose (the {@code oc-locale} cookie),
 * else their browser's, else French.
 */
@Component
public class SinglePageAppFilter implements Filter {
	static final String LOCALE_COOKIE = "oc-locale";
	static final List<String> LOCALES = List.of("fr", "en");
	static final String DEFAULT_LOCALE = "fr";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse
				&& isPageNavigation(httpRequest)) {
			var path = httpRequest.getRequestURI();
			var locale = LOCALES.stream().filter(code -> path.equals("/" + code) || path.startsWith("/" + code + "/"))
					.findFirst();

			if (locale.isPresent()) {
				request.getRequestDispatcher("/" + locale.get() + "/index.html").forward(request, response);
			} else {
				var query = Optional.ofNullable(httpRequest.getQueryString()).map(value -> "?" + value).orElse("");
				httpResponse.sendRedirect("/" + preferredLocale(httpRequest) + path + query);
			}
			return;
		}

		chain.doFilter(request, response);
	}

	private static boolean isPageNavigation(HttpServletRequest request) {
		var path = request.getRequestURI();
		var accept = request.getHeader(HttpHeaders.ACCEPT);
		return "GET".equals(request.getMethod()) && !path.startsWith("/api/") && !path.startsWith("/actuator")
				&& !path.endsWith("/index.html") && accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
	}

	static String preferredLocale(HttpServletRequest request) {
		var chosen = Optional.ofNullable(request.getCookies()).stream()
				.flatMap(Arrays::stream)
				.filter(cookie -> LOCALE_COOKIE.equals(cookie.getName()))
				.map(Cookie::getValue)
				.filter(LOCALES::contains)
				.findFirst();
		if (chosen.isPresent()) {
			return chosen.get();
		}

		var language = request.getLocale().getLanguage();
		return LOCALES.contains(language) ? language : DEFAULT_LOCALE;
	}
}
