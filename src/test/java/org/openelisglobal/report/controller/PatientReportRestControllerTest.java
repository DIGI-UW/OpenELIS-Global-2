package org.openelisglobal.report.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.report.ReportingData;
import org.openelisglobal.report.service.PatientReportService;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientReportService patientReportService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");

        UserService userServiceMock = Mockito.mock(UserService.class);
        Mockito.when(userServiceMock.filterResultsByLabUnitRoles(anyString(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1)); // Return results unfiltered

        ReflectionTestUtils.setField(patientReportService, "userService", userServiceMock);
    }

    private RequestPostProcessor mockAuthUser() {
        return request -> {
            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "adminADMIN!",
                    List.of(new SimpleGrantedAuthority("ROLE_RESULTS"))));
            request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);

            UserSessionData usd = new UserSessionData();
            usd.setSytemUserId(1);
            request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

            return request;
        };
    }

    @Test
    public void getPatientResults_shouldReturnPatientReport_whenPatientHasResults() throws Exception {

        MvcResult result = mockMvc
                .perform(get("/rest/reports/patient-results?patientId=1").with(mockAuthUser())
                        .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(200, status);

        String content = result.getResponse().getContentAsString();
        ReportingData data = super.mapFromJson(content, ReportingData.class);

        assertThat(data, notNullValue());
        assertThat(data.getColumns().size(), is(13));
        assertThat(data.getRows().size(), is(1));

        assertThat(data.getRows().get(0).getDataMap().get("accessionNumber"), is("24-00001"));

        assertThat(data.getRows().get(0).getDataMap().get("patientExternalId"), is("EXT-PAT-001"));

        assertThat(data.getRows().get(0).getDataMap().get("resultValue"), is("5.5"));
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenPatientDoesNotExist() throws Exception {

        MvcResult mvcResult = super.mockMvc.perform(get("/rest/reports/patient-results?patientId=999999")
                .with(mockAuthUser()).accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenInvalidPatientIdProvided() throws Exception {

        assertEquals(404, super.mockMvc.perform(get("/rest/reports/patient-results?patientId=-1").with(mockAuthUser()))
                .andReturn().getResponse().getStatus());

        assertEquals(404, super.mockMvc.perform(get("/rest/reports/patient-results?patientId=0").with(mockAuthUser()))
                .andReturn().getResponse().getStatus());
    }

    @Test
    public void getPatientResults_shouldReturnJsonContentType() throws Exception {

        MvcResult mvcResult = super.mockMvc.perform(get("/rest/reports/patient-results?patientId=1")
                .with(mockAuthUser()).accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
    }
}