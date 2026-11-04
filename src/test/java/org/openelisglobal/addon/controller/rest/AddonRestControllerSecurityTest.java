package org.openelisglobal.addon.controller.rest;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.addon.packaging.AddonPackageException;
import org.openelisglobal.addon.service.AddonRegistryService;
import org.openelisglobal.addon.service.AddonSettings;
import org.openelisglobal.addon.service.AddonTokenService;
import org.openelisglobal.addon.service.AddonTokenService.IssuedToken;
import org.openelisglobal.addon.service.AddonTokenService.Refusal;
import org.openelisglobal.addon.service.AddonTokenService.TokenRefusedException;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Who may do what on /rest/addons: administrators manage add-ons; any signed-in
 * user reads the catalog and the active extensions and asks for frame tokens
 * (the service then checks the slot's roles); refusals keep their codes.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, AddonRestControllerSecurityTest.TestConfig.class })
public class AddonRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AddonTokenService tokens;

    private static MockHttpSession session() {
        UserSessionData data = new UserSessionData();
        data.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, data);
        return session;
    }

    @Test
    public void listingIsForAdministrators() throws Exception {
        mockMvc.perform(get("/rest/addons").with(user("a").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.trustedPublishers[0]").value("example"));
        mockMvc.perform(get("/rest/addons").with(user("r").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void managingIsForAdministrators() throws Exception {
        MockMultipartFile pkg = new MockMultipartFile("package", "hello.zip", "application/zip", new byte[] { 1 });
        mockMvc.perform(multipart("/rest/addons").file(pkg).with(user("r").roles("RESULTS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/addons/guarded/enabled").with(user("r").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/addons/hello/config").with(user("r").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"values\":{}}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/addons/hello/credentials").with(user("r").roles("RESULTS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/addons/guarded").with(user("r").roles("RESULTS")))
                .andExpect(status().isForbidden());
        verify(registry, never()).setEnabled(eq("guarded"), anyBoolean());
        verify(registry, never()).uninstall(eq("guarded"));
    }

    @Test
    public void aRefusedPackageAnswers400WithItsCode() throws Exception {
        when(registry.install(any(), anyString())).thenThrow(new AddonPackageException("manifest.invalid",
                "addon.json is not valid"));
        MockMultipartFile pkg = new MockMultipartFile("package", "hello.zip", "application/zip", new byte[] { 1 });
        mockMvc.perform(multipart("/rest/addons").file(pkg).session(session()).with(user("a").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("manifest.invalid"));
    }

    @Test
    public void enablingNeedsABoolean() throws Exception {
        mockMvc.perform(put("/rest/addons/hello/enabled").with(user("a").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":\"yes\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/rest/addons/missing/enabled").with(user("a").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isNotFound());
        verify(registry).setEnabled(eq("missing"), eq(false));
    }

    @Test
    public void catalogAndExtensionsAreForAnySignedInUser() throws Exception {
        mockMvc.perform(get("/rest/addons/catalog").with(user("r").roles("RESULTS"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.scopes[0].key").value("orders.read"))
                .andExpect(jsonPath("$.slots[2].name").value("app-menu-slot"));
        mockMvc.perform(get("/rest/addons/extensions").with(user("r").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void tokenRefusalsKeepTheirMeaning() throws Exception {
        when(tokens.issue(eq("off"), anyString(), any(), anyString(), any()))
                .thenThrow(new TokenRefusedException(Refusal.NOT_FOUND));
        when(tokens.issue(eq("hello"), eq("result-validation-side-panel-slot"), any(), anyString(), any()))
                .thenThrow(new TokenRefusedException(Refusal.ROLE));
        when(tokens.issue(eq("hello"), eq("result-lookup-side-panel-slot"), any(), anyString(), any()))
                .thenReturn(new IssuedToken("oeat.x.y", 600, List.of("orders.read"), Map.of("page", "result-lookup")));
        String body = "{\"slot\":\"%s\",\"context\":{\"page\":\"result-lookup\"}}";
        mockMvc.perform(post("/rest/addons/off/token").session(session()).with(user("r").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(String.format(body, "result-lookup-side-panel-slot")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/rest/addons/hello/token").session(session()).with(user("r").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(body, "result-validation-side-panel-slot")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("role"));
        mockMvc.perform(post("/rest/addons/hello/token").session(session()).with(user("r").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(String.format(body, "result-lookup-side-panel-slot")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("oeat.x.y"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    public void uiFilesAreServedSandboxed() throws Exception {
        mockMvc.perform(get("/rest/addons/hello/ui/panel.html").with(user("r").roles("RESULTS")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", AddonUiRestController.FRAME_CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Type", startsWith("text/html")));
        mockMvc.perform(get("/rest/addons/hello/ui/missing.js").with(user("r").roles("RESULTS")))
                .andExpect(status().isNotFound());
    }

    @Configuration
    static class TestConfig {

        @Bean
        AddonRegistryService addonRegistryService() {
            AddonRegistryService registry = mock(AddonRegistryService.class);
            when(registry.list()).thenReturn(List.of());
            when(registry.activeExtensions()).thenReturn(List.of());
            AddonAsset page = new AddonAsset();
            page.setPath("panel.html");
            page.setContentType("text/html; charset=utf-8");
            page.setContent("<!doctype html>".getBytes());
            when(registry.asset("hello", "panel.html")).thenReturn(Optional.of(page));
            when(registry.asset("hello", "missing.js")).thenReturn(Optional.empty());
            return registry;
        }

        @Bean
        AddonTokenService addonTokenService() {
            return mock(AddonTokenService.class);
        }

        @Bean
        AddonSettings addonSettings() {
            // @Value would overwrite a field set here, so the method is overridden
            return new AddonSettings() {
                @Override
                public Map<String, byte[]> trustedPublishers() {
                    return Map.of("example", new byte[32]);
                }
            };
        }

        @Bean
        AddonAdminRestController addonAdminRestController() {
            return new AddonAdminRestController();
        }

        @Bean
        AddonUiRestController addonUiRestController() {
            return new AddonUiRestController();
        }
    }
}
