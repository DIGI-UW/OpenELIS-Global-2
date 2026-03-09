package org.openelisglobal.admin.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationImportService;
import org.openelisglobal.provider.service.ProviderImportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ContextConfiguration
public class ImportControllerTest extends BaseWebContextSensitiveTest {

    private OrganizationImportService organizationImportService;
    private ProviderImportService providerImportService;

    @Configuration
    static class TestConfig {
        @Bean
        public ImportController importController() {
            return new ImportController();
        }

        @Bean
        @Primary
        public OrganizationImportService mockOrganizationImportService() {
            return mock(OrganizationImportService.class);
        }

        @Bean
        @Primary
        public ProviderImportService mockProviderImportService() {
            return mock(ProviderImportService.class);
        }
    }

    @Before
    @Override
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        organizationImportService = webApplicationContext.getBean(OrganizationImportService.class);
        providerImportService = webApplicationContext.getBean(ProviderImportService.class);

        // Reset mocks before each test
        org.mockito.Mockito.reset(organizationImportService, providerImportService);
    }

    @Test
    public void testImportAll_Success() throws Exception {
        doNothing().when(organizationImportService).importOrganizationList();
        doNothing().when(providerImportService).importPractitionerList();

        mockMvc.perform(get("/import/all"))
                .andExpect(status().isOk());

        verify(organizationImportService).importOrganizationList();
        verify(providerImportService).importPractitionerList();
    }

    @Test
    public void testImportOrganizations_Success() throws Exception {
        doNothing().when(organizationImportService).importOrganizationList();

        mockMvc.perform(get("/import/organization"))
                .andExpect(status().isOk());

        verify(organizationImportService).importOrganizationList();
        verify(providerImportService, never()).importPractitionerList();
    }

    @Test
    public void testImportProviders_Success() throws Exception {
        doNothing().when(providerImportService).importPractitionerList();

        mockMvc.perform(get("/import/provider"))
                .andExpect(status().isOk());

        verify(providerImportService).importPractitionerList();
        verify(organizationImportService, never()).importOrganizationList();
    }

    @Test(expected = Exception.class)
    public void testImportOrganizations_ServiceThrowsException() throws Exception {
        doThrow(new IOException("FHIR connection failed"))
                .when(organizationImportService).importOrganizationList();

        mockMvc.perform(get("/import/organization"));
    }
}
