package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseAmendmentRestController;
import org.openelisglobal.microbiology.form.MicroReportVersionForm;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroReportVersionService;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroCaseAmendmentRestControllerTest {

    @Test
    public void openUsesAuthenticatedActorAndReturnsLifecycleForm() throws Exception {
        PreAuthorize open = MicroCaseAmendmentService.class
                .getMethod("openAmendment", String.class, String.class, String.class).getAnnotation(PreAuthorize.class);
        PreAuthorize cancel = MicroCaseAmendmentService.class
                .getMethod("cancelAmendment", String.class, String.class, String.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasAuthority('PRIV_MICRO_SUPERVISE')", open.value());
        assertEquals("hasAuthority('PRIV_MICRO_SUPERVISE')", cancel.value());
    }

    @Test
    public void reportHistoryIncludesNormalizedAnalysisAndResultSources() throws Exception {
        MicroCaseAmendmentService amendmentService = org.mockito.Mockito.mock(MicroCaseAmendmentService.class);
        MicroReportVersionService versionService = org.mockito.Mockito.mock(MicroReportVersionService.class);
        MicroReportVersion version = new MicroReportVersion();
        version.setId("version-1");
        version.setCaseId("case-1");
        version.setVersionNumber(1);
        MicroReportVersionSource source = new MicroReportVersionSource();
        source.setReportVersionId("version-1");
        source.setAnalysisId("42");
        source.setResultId("201");
        when(versionService.getVersions("case-1")).thenReturn(List.of(version));
        when(versionService.getSourcesForCase("case-1")).thenReturn(List.of(source));

        ResponseEntity<List<MicroReportVersionForm>> response = new MicroCaseAmendmentRestController(amendmentService,
                versionService).getReportVersions("case-1");

        assertEquals(1, response.getBody().size());
        assertEquals("42", response.getBody().get(0).sources.get(0).analysisId);
        assertEquals("201", response.getBody().get(0).sources.get(0).resultId);
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
