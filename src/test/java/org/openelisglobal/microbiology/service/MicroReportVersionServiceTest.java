package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionDAO;
import org.openelisglobal.microbiology.dao.MicroReportVersionSourceDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionType;

@RunWith(MockitoJUnitRunner.class)
public class MicroReportVersionServiceTest {

    @Mock
    private MicroReportVersionDAO reportVersionDAO;

    @Mock
    private MicroCaseAnalysisDAO caseAnalysisDAO;

    @Mock
    private MicroReportProjectionService projectionService;

    @Mock
    private MicroReportVersionSourceDAO reportVersionSourceDAO;

    @Mock
    private MicroPatientReportService patientReports;

    private MicroReportVersionService service;

    @Before
    public void setUp() {
        service = new MicroReportVersionServiceImpl(reportVersionDAO, caseAnalysisDAO, projectionService,
                reportVersionSourceDAO, patientReports);
    }

    @Test
    public void historyCompilesStructuredSnapshotsAndPreservesHistoricalText() {
        var structured = new MicroReportVersion();
        structured.setId("version-2");
        structured.setStructuredContent("snapshot-json");
        structured.setContent("internal serialized value");
        var historical = new MicroReportVersion();
        historical.setId("version-1");
        historical.setContent("Historical negative report");
        var report = new org.openelisglobal.microbiology.form.MicroPatientReportForm();
        report.labUnitName = "Culture unit";
        when(reportVersionDAO.getByCaseId("case-1")).thenReturn(List.of(historical, structured));
        when(patientReports.decode("snapshot-json")).thenReturn(report);
        var source = new MicroReportVersionSource();
        source.setReportVersionId("version-2");
        source.setAnalysisId("42");
        source.setResultId("201");
        when(reportVersionSourceDAO.getByCaseId("case-1")).thenReturn(List.of(source));

        var versions = service.getVersionForms("case-1");
        assertEquals("Historical negative report", versions.get(0).content);
        assertNull(versions.get(0).report);
        assertNull(versions.get(1).content);
        assertEquals("Culture unit", versions.get(1).report.labUnitName);
        assertEquals("42", versions.get(1).sources.get(0).analysisId);
        assertEquals("201", versions.get(1).sources.get(0).resultId);
    }

    @Test
    public void initialFinalVersionCapturesContentAndStandardRecordSources() {
        MicroCaseAnalysis link = new MicroCaseAnalysis();
        link.setAnalysisId("42");
        link.setProjectedResultId("201");
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(link));

        service.recordInitialFinal("case-1",
                new MicroReportProjectionResult("Isolate A: E. coli", true, List.of("201")), "9");

        ArgumentCaptor<MicroReportVersion> captor = ArgumentCaptor.forClass(MicroReportVersion.class);
        verify(reportVersionDAO).insert(captor.capture());
        MicroReportVersion version = captor.getValue();
        assertEquals(Integer.valueOf(1), version.getVersionNumber());
        assertEquals(MicroReportVersionType.FINAL.name(), version.getReleaseType());
        assertEquals("Isolate A: E. coli", version.getContent());
        assertNull(version.getCorrectsVersionId());
        ArgumentCaptor<MicroReportVersionSource> sourceCaptor = ArgumentCaptor.forClass(MicroReportVersionSource.class);
        verify(reportVersionSourceDAO).insert(sourceCaptor.capture());
        assertEquals("42", sourceCaptor.getValue().getAnalysisId());
        assertEquals("201", sourceCaptor.getValue().getResultId());
    }

    @Test
    public void amendedVersionCorrectsLatestVersionWithoutUpdatingIt() {
        MicroReportVersion previous = new MicroReportVersion();
        previous.setId("version-1");
        previous.setCaseId("case-1");
        previous.setVersionNumber(1);
        when(reportVersionDAO.getLatestByCaseId("case-1")).thenReturn(previous);
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of());
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");

        service.recordAmendedFinal(amendment,
                new MicroReportProjectionResult("Isolate A: K. pneumoniae", true, List.of("202")), "9");

        ArgumentCaptor<MicroReportVersion> captor = ArgumentCaptor.forClass(MicroReportVersion.class);
        verify(reportVersionDAO).insert(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getVersionNumber());
        assertEquals(MicroReportVersionType.AMENDED.name(), captor.getValue().getReleaseType());
        assertEquals("version-1", captor.getValue().getCorrectsVersionId());
        verify(reportVersionDAO, org.mockito.Mockito.never()).update(any(MicroReportVersion.class));
    }
}
