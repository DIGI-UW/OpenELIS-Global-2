package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.*;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroPatientReportForm;
import org.openelisglobal.microbiology.valueholder.*;

/** Versioned reports replace the former truncated remark-result projection. */
public class MicroReportProjectionServiceTest {
    private MicroPatientReportService reports;
    private MicroReportVersionDAO versions;
    private MicroCaseDAO cases;
    private MicroReportProjectionService service;

    @Before
    public void setUp() {
        reports = mock(MicroPatientReportService.class);
        versions = mock(MicroReportVersionDAO.class);
        cases = mock(MicroCaseDAO.class);
        service = new MicroReportProjectionServiceImpl(reports, versions, cases);
    }

    @Test
    public void partialRequiresValidatedReportableContentEvenWhenCultureIsDefinitive() {
        var report = new MicroPatientReportForm();
        report.cultures.add(new MicroPatientReportForm.Culture("culture", "Bottle", "NO_GROWTH", null, null, null));
        when(reports.preview("case")).thenReturn(report);
        assertEquals("VALIDATED_REPORTABLE_RESULT_REQUIRED",
                assertThrows(MicroAstConflictException.class, () -> service.releasePreliminary("case", "writer"))
                        .getMessage());
        verify(reports, never()).encode(any());
    }

    @Test
    public void partialRetainsStructuredComponentsWithoutRemarkTruncation() {
        var report = new MicroPatientReportForm();
        report.validatedResults = 1;
        String complete = "{\"notes\":\"" + "a".repeat(8000) + "\"}";
        when(reports.preview("case")).thenReturn(report);
        when(reports.encode(report)).thenReturn(complete);
        var projection = service.releasePreliminary("case", "writer");
        assertEquals(complete, projection.getContent());
        assertEquals(complete, projection.structuredContent);
        verifyZeroInteractions(versions, cases);
    }

    @Test
    public void finalPreviewUsesIssuedSnapshotAfterLiveCatalogChanges() {
        var c = new MicroCase();
        c.setFinalReleaseState("FINAL_RELEASED");
        var version = new MicroReportVersion();
        version.setContent("issued report");
        version.setStructuredContent("{\"classification\":\"MDR\"}");
        when(cases.get("case")).thenReturn(Optional.of(c));
        when(versions.getLatestByCaseId("case")).thenReturn(version);
        var projection = service.preview("case");
        assertEquals("issued report", projection.getContent());
        assertEquals("{\"classification\":\"MDR\"}", projection.structuredContent);
        verify(reports, never()).preview(anyString());
    }

    @Test public void openCasePreviewDoesNotCreateAVersion() {
        when(cases.get("case")).thenReturn(Optional.of(new MicroCase()));
        var report=new MicroPatientReportForm();when(reports.preview("case")).thenReturn(report);when(reports.encode(report)).thenReturn("{}");
        assertEquals("{}",service.preview("case").getContent());
        verifyZeroInteractions(versions);
    }
}
