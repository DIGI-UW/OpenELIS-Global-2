package org.openelisglobal.delivery.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.delivery.dto.SentMessageDTO;
import org.openelisglobal.delivery.dto.SentMessagesPageDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliverySettings;
import org.openelisglobal.delivery.service.DeliverySettingsService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Role assertions for /rest/delivery: channels and settings belong to
 * administrators, the Sent Messages list and Resend to report users and
 * administrators, and a bench user sees none of it.
 *
 * <p>
 * BaseWebContextSensitiveTest cannot pin any of this: its AppTestConfig
 * excludes SecurityConfig, so every {@code @PreAuthorize} is inert there.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, DeliveryRestControllerSecurityTest.TestConfig.class })
public class DeliveryRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void channels_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/delivery/channels")).andExpect(status().isUnauthorized());
    }

    @Test
    public void channels_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/delivery/channels").with(user("a").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void channels_reportsRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/delivery/channels").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveChannel_reportsRole_returns403() throws Exception {
        mockMvc.perform(put("/rest/delivery/channels/email").with(user("rep").roles("REPORTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testChannel_reportsRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/delivery/channels/email/test").with(user("rep").roles("REPORTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"address\":\"lab@example.org\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void settings_reportsRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/delivery/settings").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/delivery/settings").with(user("rep").roles("REPORTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"holdMinutes\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void settings_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/delivery/settings").with(user("a").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void messages_reportsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/delivery/messages").with(user("rep").roles("REPORTS"))).andExpect(status().isOk());
        mockMvc.perform(get("/rest/delivery/messages/7").with(user("rep").roles("REPORTS"))).andExpect(status().isOk());
    }

    @Test
    public void messages_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/delivery/messages").with(user("a").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void messages_resultsRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/delivery/messages").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/delivery/messages/7").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void resend_reportsRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/delivery/messages/7/resend").session(sessionForSysUser(1))
                .with(user("rep").roles("REPORTS"))).andExpect(status().isOk());
    }

    @Test
    public void resend_resultsRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/delivery/messages/7/resend").session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    /**
     * ControllerUtills.getSysUserId falls back to a SpringContext bean this slice
     * has no context for, so hand it the session attribute the login flow sets.
     */
    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    @Configuration
    static class TestConfig {

        @Bean
        DeliveryChannelService deliveryChannelService() {
            DeliveryChannelService service = mock(DeliveryChannelService.class);
            when(service.list()).thenReturn(List.of());
            return service;
        }

        @Bean
        DeliverySettingsService deliverySettingsService() {
            DeliverySettingsService service = mock(DeliverySettingsService.class);
            when(service.get()).thenReturn(new DeliverySettings());
            return service;
        }

        @Bean
        DeliveryOutboxService deliveryOutboxService() {
            DeliveryOutboxService service = mock(DeliveryOutboxService.class);
            SentMessagesPageDTO page = new SentMessagesPageDTO();
            page.setRows(List.of());
            when(service.list(any(), anyInt(), anyInt())).thenReturn(page);
            SentMessageDTO detail = new SentMessageDTO();
            detail.setId(7L);
            when(service.detail(anyLong())).thenReturn(Optional.of(detail));
            NotificationLog queued = new NotificationLog();
            queued.setId(7L);
            when(service.resend(anyLong(), anyString())).thenReturn(queued);
            return service;
        }

        @Bean
        DeliveryRestController deliveryRestController() {
            return new DeliveryRestController();
        }
    }
}
