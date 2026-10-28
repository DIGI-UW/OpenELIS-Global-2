package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

public class ForwardedRequestFilterTest {

    private static final String PROXY = "172.18.0.5";

    private static MockHttpServletRequest requestThroughProxy(String remoteAddr, String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/OpenELIS-Global/Dashboard");
        request.setContextPath("/OpenELIS-Global");
        request.setScheme("https");
        request.setSecure(true);
        request.setServerPort(443);
        request.setRemoteAddr(remoteAddr);
        request.addHeader("Host", host);
        return request;
    }

    private static HttpServletRequest filter(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        new ForwardedRequestFilter().doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }

    @Test
    public void forwardedPortFromATrustedProxyIsKeptInTheRequestUrl() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Port", "8443");

        HttpServletRequest forwarded = filter(request);

        assertEquals("lab.example.org", forwarded.getServerName());
        assertEquals(8443, forwarded.getServerPort());
        assertEquals("https://lab.example.org:8443/OpenELIS-Global/Dashboard", forwarded.getRequestURL().toString());
    }

    @Test
    public void rfc7239ForwardedHeaderGivesTheHostAndPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "oe.openelis.org:8443");
        request.addHeader("Forwarded", "for=192.0.2.60;proto=https;host=\"lab.example.org:8443\", for=10.0.0.2");

        HttpServletRequest forwarded = filter(request);

        assertEquals("https", forwarded.getScheme());
        assertEquals("lab.example.org", forwarded.getServerName());
        assertEquals(8443, forwarded.getServerPort());
    }

    @Test
    public void forwardedHostCarriesItsOwnPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "oe.openelis.org:8443");
        request.addHeader("X-Forwarded-Host", "lab.example.org:9443");

        HttpServletRequest forwarded = filter(request);

        assertEquals("lab.example.org", forwarded.getServerName());
        assertEquals(9443, forwarded.getServerPort());
    }

    @Test
    public void forwardedHttpOnAPlainPortUsesTheDefaultPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Proto", "http");

        HttpServletRequest forwarded = filter(request);

        assertEquals("http", forwarded.getScheme());
        assertFalse(forwarded.isSecure());
        assertEquals(80, forwarded.getServerPort());
        assertEquals("http://lab.example.org/OpenELIS-Global/Dashboard", forwarded.getRequestURL().toString());
    }

    @Test
    public void portInTheHostHeaderSurvivesAForwardedProtocol() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "localhost:55461");
        request.addHeader("X-Forwarded-Proto", "https");

        HttpServletRequest forwarded = filter(request);

        assertEquals("localhost", forwarded.getServerName());
        assertEquals(55461, forwarded.getServerPort());
    }

    @Test
    public void forwardedHostThatIsNotAHostNameIsIgnored() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org:8443");
        request.addHeader("X-Forwarded-Host", "__");

        HttpServletRequest forwarded = filter(request);

        assertEquals("lab.example.org", forwarded.getServerName());
        assertEquals(8443, forwarded.getServerPort());
    }

    @Test
    public void forwardedHostLongerThanAHostNameIsIgnored() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org:8443");
        request.addHeader("X-Forwarded-Host", "a".repeat(254));

        HttpServletRequest forwarded = filter(request);

        assertEquals("lab.example.org", forwarded.getServerName());
        assertEquals(8443, forwarded.getServerPort());
    }

    @Test
    public void malformedForwardedValuesAreIgnored() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org:8443");
        request.addHeader("X-Forwarded-Proto", "gopher");
        request.addHeader("X-Forwarded-Port", "99999");

        HttpServletRequest forwarded = filter(request);

        assertEquals("https", forwarded.getScheme());
        assertEquals(8443, forwarded.getServerPort());
    }

    @Test
    public void forwardedHeadersFromAnUntrustedAddressAreIgnored() throws Exception {
        MockHttpServletRequest request = requestThroughProxy("203.0.113.9", "lab.example.org");
        request.addHeader("X-Forwarded-Port", "8443");
        request.addHeader("X-Forwarded-Host", "evil.example.com");

        HttpServletRequest forwarded = filter(request);

        assertSame(request, forwarded);
        assertEquals(443, forwarded.getServerPort());
    }

    @Test
    public void clientAddressIsTheConnectionsAddress() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Port", "8443");
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        request.addHeader("Forwarded", "for=198.51.100.7");

        HttpServletRequest forwarded = filter(request);

        assertEquals(PROXY, forwarded.getRemoteAddr());
        assertEquals("198.51.100.7", forwarded.getHeader("X-Forwarded-For"));
    }

    @Test
    public void loginRedirectKeepsTheForwardedPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Port", "8443");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new LoginUrlAuthenticationEntryPoint("/LoginPage").commence(filter(request), response,
                new InsufficientAuthenticationException("login required"));

        assertEquals("https://lab.example.org:8443/OpenELIS-Global/LoginPage", response.getRedirectedUrl());
        assertEquals(302, response.getStatus());
    }

    @Test
    public void logoutRedirectKeepsTheForwardedPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Port", "8443");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SimpleUrlLogoutSuccessHandler logout = new SimpleUrlLogoutSuccessHandler();
        logout.setDefaultTargetUrl("/LoginPage");

        new ForwardedRequestFilter().doFilter(request, response,
                (req, res) -> logout.onLogoutSuccess((HttpServletRequest) req, (HttpServletResponse) res, null));

        assertEquals("https://lab.example.org:8443/OpenELIS-Global/LoginPage", response.getRedirectedUrl());
    }

    @Test
    public void redirectRelativeToThePageKeepsTheForwardedPort() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Port", "8443");

        assertEquals("https://lab.example.org:8443/OpenELIS-Global/Home?access=denied",
                redirect(request, "Home?access=denied").getRedirectedUrl());
    }

    @Test
    public void redirectToAnotherSiteIsLeftAlone() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org");
        request.addHeader("X-Forwarded-Port", "8443");

        assertEquals("https://idp.example.com/sso",
                redirect(request, "https://idp.example.com/sso").getRedirectedUrl());
    }

    @Test
    public void redirectWithoutForwardedHeadersIsLeftAlone() throws Exception {
        MockHttpServletRequest request = requestThroughProxy(PROXY, "lab.example.org:8443");

        assertEquals("/OpenELIS-Global/LoginPage", redirect(request, "/OpenELIS-Global/LoginPage").getRedirectedUrl());
    }

    private static MockHttpServletResponse redirect(MockHttpServletRequest request, String location) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ForwardedRequestFilter().doFilter(request, response,
                (req, res) -> ((HttpServletResponse) res).sendRedirect(location));
        return response;
    }
}
