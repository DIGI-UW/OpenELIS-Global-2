package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The module authorization lookup used the raw request URI, so a
 * percent-encoded character skipped the lookup while Spring still routed the
 * request to the controller (SCAN-049).
 */
public class URLUtilTest {

    @Test
    public void anEncodedPath_resolvesToTheSameModuleKeyAsThePlainOne() {
        assertEquals("/UnifiedSystemUser", URLUtil.getReourcePathFromRequest(request("/rest/Unified%53ystemUser")));
        assertEquals("/UnifiedSystemUser", URLUtil.getReourcePathFromRequest(request("/rest/UnifiedSystemUser")));
    }

    @Test
    public void pathParametersAndDotSegments_doNotChangeTheKey() {
        assertEquals("/UnifiedSystemUser",
                URLUtil.getReourcePathFromRequest(request("/rest/UnifiedSystemUser;jsessionid=abc")));
        assertEquals("/UnifiedSystemUser",
                URLUtil.getReourcePathFromRequest(request("/rest/other/../UnifiedSystemUser")));
    }

    @Test
    public void theContextPath_isNotPartOfTheKey() {
        MockHttpServletRequest request = request("/OpenELIS-Global/rest/Unified%53ystemUser");
        request.setContextPath("/OpenELIS-Global");
        assertEquals("/UnifiedSystemUser", URLUtil.getReourcePathFromRequest(request));
        assertEquals("/rest/UnifiedSystemUser", URLUtil.getRequestPath(request));
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath("");
        return request;
    }
}
