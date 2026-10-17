package dev.daniellavoie.opencabinet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;

class SinglePageAppFilterTest {
	private final SinglePageAppFilter filter = new SinglePageAppFilter();

	@Test
	void pagesInsideALanguageGetItsIndex() throws Exception {
		var response = navigate("/en/projects/42", null, Locale.CANADA_FRENCH);

		assertThat(response.getForwardedUrl()).isEqualTo("/en/index.html");
	}

	@Test
	void otherPagesRedirectToTheChosenLanguage() throws Exception {
		var response = navigate("/projects", new Cookie("oc-locale", "en"), Locale.CANADA_FRENCH);

		assertThat(response.getRedirectedUrl()).isEqualTo("/en/projects?tab=1");
	}

	@Test
	void browserLanguageThenFrenchDecideOtherwise() throws Exception {
		assertThat(navigate("/", null, Locale.ENGLISH).getRedirectedUrl()).isEqualTo("/en/?tab=1");
		assertThat(navigate("/", null, Locale.GERMAN).getRedirectedUrl()).isEqualTo("/fr/?tab=1");
	}

	@Test
	void apiAndAssetsPassThrough() throws Exception {
		var request = new MockHttpServletRequest("GET", "/fr/main-ABC.js");
		request.addHeader("Accept", "*/*");
		var chain = new MockFilterChain();

		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertThat(chain.getRequest()).isSameAs(request);
	}

	private MockHttpServletResponse navigate(String path, Cookie cookie, Locale locale) throws Exception {
		var request = new MockHttpServletRequest("GET", path);
		request.setQueryString("tab=1");
		request.addHeader("Accept", "text/html,application/xhtml+xml");
		request.addPreferredLocale(locale);
		if (cookie != null) {
			request.setCookies(cookie);
		}
		var response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}
}
