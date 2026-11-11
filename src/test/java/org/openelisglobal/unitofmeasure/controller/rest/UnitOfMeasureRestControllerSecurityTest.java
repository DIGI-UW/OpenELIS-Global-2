package org.openelisglobal.unitofmeasure.controller.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Order entry and the test editor read the unit list from GET /rest/uom, so any
 * signed-in user may read it, admin or not; adding or editing a unit stays an
 * admin action.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { UnitOfMeasureRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class UnitOfMeasureRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Before
    public void stubUnits() {
        reset(unitOfMeasureService);
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setId("1");
        unit.setUnitOfMeasureName("mg/dL");
        when(unitOfMeasureService.getAll()).thenReturn(List.of(unit));
    }

    @Test
    public void unitList_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/uom")).andExpect(status().isUnauthorized());
    }

    @Test
    public void unitList_NonAdminUser_ReturnsTheUnits() throws Exception {
        mockMvc.perform(get("/rest/uom").with(user("reception").roles("RECEPTION"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("mg/dL"));
    }

    @Test
    public void unitList_AdminUser_ReturnsTheUnits() throws Exception {
        mockMvc.perform(get("/rest/uom").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("mg/dL"));
    }

    @Test
    public void addingOrEditingAUnit_NonAdminUser_Returns403() throws Exception {
        mockMvc.perform(post("/rest/uom").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"g/L\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/uom/1").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"g/L\"}"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        org.springframework.security.web.SecurityFilterChain securityFilterChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        UnitOfMeasureService unitOfMeasureService() {
            return mock(UnitOfMeasureService.class);
        }

        @Bean
        UnitOfMeasureRestController unitOfMeasureRestController(UnitOfMeasureService unitOfMeasureService) {
            UnitOfMeasureRestController controller = new UnitOfMeasureRestController();
            ReflectionTestUtils.setField(controller, "unitOfMeasureService", unitOfMeasureService);
            return controller;
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }
    }
}
