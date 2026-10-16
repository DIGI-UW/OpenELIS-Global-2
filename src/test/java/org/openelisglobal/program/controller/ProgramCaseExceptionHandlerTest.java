package org.openelisglobal.program.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * A case save the service refuses (here a technician without the Results role
 * on the case's lab unit) answers 400 with the reason, even with the
 * application's global handler present, which answers any other runtime error
 * with a 500.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { ProgramCaseExceptionHandlerTest.TestConfig.class })
public class ProgramCaseExceptionHandlerTest extends SecuritySliceMockMvcTest {

    @Autowired
    private PathologySampleService pathologySampleService;

    @Before
    public void resetMocks() {
        reset(pathologySampleService);
    }

    /** The signed-in user's OpenELIS session data, as a real request carries it. */
    private static UserSessionData signedIn() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        return usd;
    }

    @Test
    public void aRefusedTechnicianIsABadRequestWithTheReason() throws Exception {
        doThrow(new IllegalArgumentException("The technician must have the Results role for Pathology"))
                .when(pathologySampleService).updateWithFormValues(eq(5), isA(PathologySampleForm.class));

        MvcResult result = mockMvc
                .perform(post("/rest/pathology/caseView/5").with(user("u").roles("ADMIN"))
                        .requestAttr(IActionConstants.USER_SESSION_DATA, signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assignedTechnicianId\":\"99\"}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString(),
                result.getResponse().getContentAsString().contains("Results role for Pathology"));
    }

    @Test
    public void otherFailuresStayServerErrors() throws Exception {
        doThrow(new IllegalStateException("database down")).when(pathologySampleService).updateWithFormValues(eq(6),
                isA(PathologySampleForm.class));

        MvcResult result = mockMvc.perform(post("/rest/pathology/caseView/6").with(user("u").roles("ADMIN"))
                .requestAttr(IActionConstants.USER_SESSION_DATA, signedIn()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn();

        assertEquals(500, result.getResponse().getStatus());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        PathologySampleService pathologySampleService() {
            return mock(PathologySampleService.class);
        }

        @Bean
        PathologyDisplayService pathologyDisplayService() {
            return mock(PathologyDisplayService.class);
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        @Bean
        PathologyController pathologyController() {
            return new PathologyController();
        }

        // the application's global handler (500 for any runtime error) and ours
        @Bean
        ControllerSetup controllerSetup() {
            return new ControllerSetup();
        }

        @Bean
        ProgramCaseExceptionHandler programCaseExceptionHandler() {
            return new ProgramCaseExceptionHandler();
        }
    }
}
