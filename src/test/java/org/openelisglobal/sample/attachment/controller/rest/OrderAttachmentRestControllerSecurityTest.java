package org.openelisglobal.sample.attachment.controller.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.attachment.service.OrderAttachmentService;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { OrderAttachmentRestControllerSecurityTest.TestConfig.class })
public class OrderAttachmentRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    private static MockMultipartFile testFile() {
        return new MockMultipartFile("files", "test.pdf", "application/pdf", new byte[] { 1, 2, 3 });
    }

    // --- uploadAttachments (POST /rest/order/{accessionNumber}/attachments) ---

    @Test
    public void uploadAttachments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void uploadAttachments_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()).with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void uploadAttachments_receptionRole_returns200() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()).session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION"))).andExpect(status().isOk());
    }

    @Test
    public void uploadAttachments_resultsRole_returns200() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()).session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void uploadAttachments_validationRole_returns200() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()).session(sessionForSysUser(1))
                .with(user("res").roles("VALIDATION"))).andExpect(status().isOk());
    }

    @Test
    public void uploadAttachments_adminRole_returns200() throws Exception {
        mockMvc.perform(multipart("/rest/order/ACC123/attachments").file(testFile()).session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN"))).andExpect(status().isOk());
    }

    // --- listAttachments (GET /rest/order/{accessionNumber}/attachments) ---

    @Test
    public void listAttachments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments")).andExpect(status().isUnauthorized());
    }

    @Test
    public void listAttachments_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void listAttachments_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments").with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void listAttachments_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments").with(user("res").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void listAttachments_validationRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments").with(user("res").roles("VALIDATION")))
                .andExpect(status().isOk());
    }

    @Test
    public void listAttachments_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/ACC123/attachments").with(user("adm").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // --- downloadAttachment (GET /rest/order/attachments/{attachmentId}/download)
    // ---

    @Test
    public void downloadAttachment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download")).andExpect(status().isUnauthorized());
    }

    @Test
    public void downloadAttachment_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void downloadAttachment_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download").with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void downloadAttachment_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download").with(user("res").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void downloadAttachment_validationRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download").with(user("res").roles("VALIDATION")))
                .andExpect(status().isOk());
    }

    @Test
    public void downloadAttachment_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/download").with(user("adm").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // --- viewAttachment (GET /rest/order/attachments/{attachmentId}/view) ---

    @Test
    public void viewAttachment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view")).andExpect(status().isUnauthorized());
    }

    @Test
    public void viewAttachment_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void viewAttachment_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view").with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void viewAttachment_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view").with(user("res").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void viewAttachment_validationRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view").with(user("res").roles("VALIDATION")))
                .andExpect(status().isOk());
    }

    @Test
    public void viewAttachment_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/order/attachments/1/view").with(user("adm").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    // --- deleteAttachment (DELETE /rest/order/attachments/{attachmentId}) ---

    @Test
    public void deleteAttachment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/rest/order/attachments/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteAttachment_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(delete("/rest/order/attachments/1").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteAttachment_receptionRole_returns200() throws Exception {
        mockMvc.perform(
                delete("/rest/order/attachments/1").session(sessionForSysUser(1)).with(user("rec").roles("RECEPTION")))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteAttachment_resultsRole_returns403() throws Exception {
        mockMvc.perform(
                delete("/rest/order/attachments/1").session(sessionForSysUser(1)).with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteAttachment_adminRole_returns200() throws Exception {
        mockMvc.perform(
                delete("/rest/order/attachments/1").session(sessionForSysUser(1)).with(user("adm").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        SampleService sampleService() {
            SampleService service = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ACC123");
            when(service.getSampleByAccessionNumber("ACC123")).thenReturn(sample);
            return service;
        }

        @Bean
        OrderAttachmentService orderAttachmentService() {
            OrderAttachmentService service = mock(OrderAttachmentService.class);
            OrderAttachment attachment = new OrderAttachment();
            attachment.setId(1);
            attachment.setOriginalFileName("test.pdf");
            attachment.setFileType("application/pdf");
            attachment.setFileContent(new byte[] { 1, 2, 3 });
            attachment.setIsDeleted(false);
            when(service.get(1)).thenReturn(attachment);
            when(service.findActiveBySampleId(1L)).thenReturn(List.of(attachment));
            return service;
        }

        @Bean
        OrderAttachmentRestController orderAttachmentRestController(OrderAttachmentService orderAttachmentService,
                SampleService sampleService) {
            OrderAttachmentRestController controller = new OrderAttachmentRestController();
            ReflectionTestUtils.setField(controller, "orderAttachmentService", orderAttachmentService);
            ReflectionTestUtils.setField(controller, "sampleService", sampleService);
            return controller;
        }
    }
}
