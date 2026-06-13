package org.openelisglobal.patient.controller;

import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ContextConfiguration
public abstract class PatientControllerTestBase extends BaseWebContextSensitiveTest {

    @Configuration
    static class Config {
        @Bean
        public PatientManagementController patientManagementController() {
            return new PatientManagementController();
        }
    }

    @Override
    public void setUp() throws Exception {
        // We need to rebuild mockMvc because the base one from super.setUp()
        // won't have the manually registered controller from the inner @Configuration
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }
}
