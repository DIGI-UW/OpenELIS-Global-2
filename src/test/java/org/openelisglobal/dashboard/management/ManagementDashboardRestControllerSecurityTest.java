package org.openelisglobal.dashboard.management;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.dashboard.management.controller.rest.ManagementDashboardRestController;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.openelisglobal.dashboard.management.workload.WorkloadService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        ManagementDashboardRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class ManagementDashboardRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void benchRoleWithoutTheGrantIsRefused() throws Exception {
        mockMvc.perform(get("/rest/dashboards/management/summary").with(user("bench").roles("RESULTS", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void qaAuthorityIsNotTheDashboardGrant() throws Exception {
        mockMvc.perform(get("/rest/dashboards/management/summary")
                .with(user("qa").authorities(new SimpleGrantedAuthority("qa.view.overview"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void dashboardGrantIsAdmitted() throws Exception {
        mockMvc.perform(get("/rest/dashboards/management/summary?scope=7d")
                .with(user("manager").authorities(new SimpleGrantedAuthority("dashboards.view.management"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.labName").value("Test Lab"));
    }

    @Test
    public void globalAdminIsAdmitted() throws Exception {
        mockMvc.perform(get("/rest/dashboards/management/summary").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void unknownScopeIsABadRequest() throws Exception {
        mockMvc.perform(
                get("/rest/dashboards/management/summary?scope=decade").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void workloadRefusesABenchRoleWithoutTheGrant() throws Exception {
        mockMvc.perform(get("/rest/dashboards/management/workload").with(user("bench").roles("RESULTS", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void workloadWithAnUnknownScopeIsABadRequest() throws Exception {
        mockMvc.perform(
                get("/rest/dashboards/management/workload?scope=decade").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Configuration
    static class TestConfig {

        @Bean
        ManagementDashboardService managementDashboardService() {
            return scope -> new ManagementSummary("Test Lab", 0, Map.of());
        }

        @Bean
        KpiService kpiService() {
            return new KpiService() {
                @Override
                public ManagementKpis getKpis(ScopeRange scope) {
                    return null;
                }

                @Override
                public Map<String, BigDecimal> onTimeByLabUnit(ScopeRange scope) {
                    return Map.of();
                }
            };
        }

        @Bean
        WorkloadService workloadService() {
            return scope -> null;
        }

        @Bean
        ManagementDashboardRestController managementDashboardRestController() {
            return new ManagementDashboardRestController();
        }
    }
}
