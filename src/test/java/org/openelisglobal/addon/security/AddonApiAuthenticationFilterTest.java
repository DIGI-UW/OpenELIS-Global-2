package org.openelisglobal.addon.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class AddonApiAuthenticationFilterTest {

    private static final List<String> COMPOSE = List.of("172.16.0.0/12", "127.0.0.0/8");

    private static MockHttpServletRequest from(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/rest/addon-api/v1/whoami");
        request.setRemoteAddr(address);
        return request;
    }

    @Test
    public void aContainerOnTheComposeNetworkIsAllowed() {
        assertTrue(AddonApiAuthenticationFilter.fromAllowedNetwork(from("172.20.1.130"), COMPOSE));
    }

    @Test
    public void aPublicAddressIsRefused() {
        assertFalse(AddonApiAuthenticationFilter.fromAllowedNetwork(from("203.0.113.9"), COMPOSE));
    }

    @Test
    public void aRequestRelayedByTheReverseProxyIsRefused() {
        for (String header : List.of("X-Forwarded-For", "X-Forwarded-Host", "Forwarded", "X-Real-IP")) {
            MockHttpServletRequest request = from("172.20.1.2");
            request.addHeader(header, "198.51.100.4");
            assertFalse(header, AddonApiAuthenticationFilter.fromAllowedNetwork(request, COMPOSE));
        }
    }

    @Test
    public void aMalformedRangeMatchesNothing() {
        assertFalse(AddonApiAuthenticationFilter.fromAllowedNetwork(from("172.20.1.130"), List.of("not-a-cidr")));
    }
}
