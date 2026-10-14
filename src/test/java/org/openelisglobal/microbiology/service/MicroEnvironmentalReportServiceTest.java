package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.form.MicroPatientReportForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.security.access.AccessDeniedException;

public class MicroEnvironmentalReportServiceTest {
    @Test
    public void certificateUsesLatestReleasedSnapshotAndChecksEveryCaseUnit() {
        var cases = mock(MicroCaseDAO.class);
        var reports = mock(MicroPatientReportService.class);
        var access = mock(MicrobiologyCaseAccessService.class);
        var service = new MicroEnvironmentalReportService(cases, reports, access);
        var c = new MicroCase();
        c.setId("case");
        c.setSiteId("1");
        c.setLabUnitId("2");
        c.setFinalReleaseState("FINAL_RELEASED");
        when(cases.getByOrder("order")).thenReturn(List.of(c));
        assertThrows(AccessDeniedException.class, () -> service.released("order", "actor"));
        verifyZeroInteractions(reports);
        when(access.hasLabUnitRole("actor", "2", Constants.ROLE_REPORTS)).thenReturn(true);
        var finalReport = new MicroPatientReportForm();
        when(reports.versions("case")).thenReturn(
                List.of(new MicroPatientReportForm.Version("v1", 1, "FINAL", null, "validator", finalReport),
                        new MicroPatientReportForm.Version("v2", 2, "PRELIMINARY", null, "validator",
                                new MicroPatientReportForm())));
        assertSame(finalReport, service.released("order", "actor").get(0));
        c.setFinalReleaseState("NOT_RELEASED");
        assertThrows(IllegalStateException.class, () -> service.released("order", "actor"));
    }

    @Test
    public void clinicalCasesCannotBePrintedAsEnvironmentalCertificates() {
        var cases = mock(MicroCaseDAO.class);
        when(cases.getByOrder("order")).thenReturn(List.of(new MicroCase()));
        var reports = mock(MicroPatientReportService.class);
        var service = new MicroEnvironmentalReportService(cases, reports, mock(MicrobiologyCaseAccessService.class));
        assertThrows(IllegalArgumentException.class, () -> service.released("order", "actor"));
        verifyZeroInteractions(reports);
    }
}
