package org.openelisglobal.module.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

public class ModuleAssetsControllerTest {

    private static MockHttpServletRequest request(String pathWithinMapping) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", pathWithinMapping);
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
        return request;
    }

    @Test
    public void pathBelowTheModuleDirectoryIsAccepted() {
        assertEquals("remoteEntry.js", ModuleAssetsController.assetPath(request("/modules/ctx/remoteEntry.js"), "ctx"));
        assertEquals("assets/Page-ab12cd.js",
                ModuleAssetsController.assetPath(request("/modules/ctx/assets/Page-ab12cd.js"), "ctx"));
        assertEquals("messages/en.json",
                ModuleAssetsController.assetPath(request("/modules/ctx/messages/en.json"), "ctx"));
    }

    @Test
    public void pathsThatLeaveTheDirectoryAreRefused() {
        assertNull(ModuleAssetsController.assetPath(request("/modules/ctx/../other/x.js"), "ctx"));
        assertNull(ModuleAssetsController.assetPath(request("/modules/ctx//etc/passwd"), "ctx"));
        assertNull(ModuleAssetsController.assetPath(request("/modules/ctx/"), "ctx"));
        assertNull(ModuleAssetsController.assetPath(request("/modules/other/remoteEntry.js"), "ctx"));
    }

    @Test
    public void mediaTypesAndCachingFollowTheFileName() {
        assertEquals(MediaType.valueOf("text/javascript"), ModuleAssetsController.mediaType("remoteEntry.js"));
        assertEquals(MediaType.APPLICATION_JSON, ModuleAssetsController.mediaType("messages/fr.json"));
        assertEquals(MediaType.valueOf("text/css"), ModuleAssetsController.mediaType("assets/style-1a2b3c.css"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, ModuleAssetsController.mediaType("assets/font.bin"));
        assertTrue(ModuleAssetsController.isImmutable("assets/Page-ab12cd.js"));
        assertTrue(ModuleAssetsController.isImmutable("assets/style-BxQ9kL0z.css"));
        assertFalse(ModuleAssetsController.isImmutable("remoteEntry.js"));
        assertFalse(ModuleAssetsController.isImmutable("messages/en.json"));
    }
}
