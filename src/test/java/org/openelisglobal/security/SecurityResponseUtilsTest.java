package org.openelisglobal.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class SecurityResponseUtilsTest {

    @Test
    public void isHtmlRequest_browserAcceptHeader_returnsTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

        assertTrue(SecurityResponseUtils.isHtmlRequest(request));
    }

    @Test
    public void isHtmlRequest_fetchDefaultAcceptHeader_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "*/*");

        assertFalse(SecurityResponseUtils.isHtmlRequest(request));
    }

    @Test
    public void isHtmlRequest_explicitJsonAcceptHeader_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");

        assertFalse(SecurityResponseUtils.isHtmlRequest(request));
    }

    @Test
    public void isHtmlRequest_noAcceptHeader_returnsFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertFalse(SecurityResponseUtils.isHtmlRequest(request));
    }
}
