package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseAmendmentRestController;
import org.openelisglobal.microbiology.form.MicroCaseAmendmentForm;
import org.openelisglobal.microbiology.form.MicroCaseAmendmentRequestForm;
import org.openelisglobal.microbiology.form.MicroReportVersionForm;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroReportVersionService;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroCaseAmendmentRestControllerTest {

    @Test
    public void openUsesAuthenticatedActorAndReturnsLifecycleForm() {
        MicroCaseAmendmentService amendmentService = org.mockito.Mockito.mock(MicroCaseAmendmentService.class);
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");
        amendment.setSequenceNumber(1);
        amendment.setStatus("OPEN");
        amendment.setReason("Correct identification");
        amendment.setOpenedBy("42");
        when(amendmentService.openAmendment(eq("case-1"), eq("Correct identification"), eq("42")))
                .thenReturn(amendment);
        MicroCaseAmendmentRequestForm request = new MicroCaseAmendmentRequestForm();
        request.reason = "Correct identification";

        ResponseEntity<MicroCaseAmendmentForm> response = new MicroCaseAmendmentRestController(amendmentService,
                org.mockito.Mockito.mock(MicroReportVersionService.class)).open("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("amendment-1", response.getBody().id);
        assertEquals("42", response.getBody().openedBy);
        verify(amendmentService).openAmendment("case-1", "Correct identification", "42");
    }

    @Test
    public void cancelUsesAuthenticatedActorAndReason() {
        MicroCaseAmendmentService amendmentService = org.mockito.Mockito.mock(MicroCaseAmendmentService.class);
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");
        amendment.setStatus("CANCELLED");
        when(amendmentService.cancelAmendment("case-1", "Not required", "42")).thenReturn(amendment);
        MicroCaseAmendmentRequestForm request = new MicroCaseAmendmentRequestForm();
        request.reason = "Not required";

        new MicroCaseAmendmentRestController(amendmentService,
                org.mockito.Mockito.mock(MicroReportVersionService.class)).cancel("case-1", request, requestFor("42"));

        verify(amendmentService).cancelAmendment("case-1", "Not required", "42");
    }

    @Test
    public void amendmentWritesRequireSupervisorOrAdminRole() throws Exception {
        PreAuthorize open = MicroCaseAmendmentRestController.class.getMethod("open", String.class,
                MicroCaseAmendmentRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize cancel = MicroCaseAmendmentRestController.class.getMethod("cancel", String.class,
                MicroCaseAmendmentRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasAnyRole('ADMIN', 'VALIDATION')", open.value());
        assertEquals("hasAnyRole('ADMIN', 'VALIDATION')", cancel.value());
    }

    @Test
    public void reportHistoryIncludesNormalizedAnalysisAndResultSources() {
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
