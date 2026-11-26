package org.openelisglobal.logo.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import org.junit.Test;
import org.openelisglobal.common.service.servlet.reports.LogoUploadService;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * General Configuration's logo upload page uses this controller. It is in the
 * Global Administrator-only admin area (issue #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { LogoUploadRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LogoUploadRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void uploadLogo_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void uploadLogo_nonAdminReturns403() throws Exception {
        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo")
                .with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void uploadLogo_adminPassesAuthorization() throws Exception {
        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo")
                .with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void uploadLogo_receptionReturns403() throws Exception {
        mockMvc.perform(multipart("/rest/logoUpload").file(logoFile()).param("logoName", "headerLogo")
                .with(user("reception").roles("RECEPTION"))).andExpect(status().isForbidden());
    }

    private static MockMultipartFile logoFile() {
        byte[] png = Base64.getDecoder()
                .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN9sAAAAASUVORK5CYII=");
        return new MockMultipartFile("logoFile", "logo.png", MediaType.IMAGE_PNG_VALUE, png);
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
        ImageService imageService() {
            return mock(ImageService.class);
        }

        @Bean
        SiteInformationService siteInformationService() {
            return mock(SiteInformationService.class);
        }

        @Bean
        LogoUploadService logoUploadService() {
            LogoUploadService service = mock(LogoUploadService.class);
            // Exercise the handler's rejection path without writing a preview file.
            doThrow(new IllegalArgumentException("Logo rejected by test collaborator")).when(service)
                    .validateLogo(any(MultipartFile.class));
            return service;
        }

        @Bean
        LogoUploadRestController logoUploadRestController() {
            return new LogoUploadRestController();
        }
    }
}
