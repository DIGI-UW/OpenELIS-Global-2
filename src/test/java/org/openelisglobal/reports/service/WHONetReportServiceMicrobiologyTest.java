package org.openelisglobal.reports.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroWhonetExportRunDAO;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.service.MicroWhonetDataset;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicroWhonetExportBlockedException;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;

@RunWith(MockitoJUnitRunner.class)
public class WHONetReportServiceMicrobiologyTest {

    @Mock
    private MicroWhonetDatasetService datasetService;
    @Mock
    private MicroWhonetExportRunDAO exportRunDAO;

    private WHONetReportService service;

    @Before
    public void setUp() {
        service = new WHONetReportServiceImpl(datasetService, exportRunDAO);
    }

    @Test
    public void generationUsesExistingCsvContractAndAuditsAuthenticatedActor() {
        MicroWhonetExportQueryForm query = query();
        MicroWhonetPreviewForm preview = preview(true);
        WHONetRow row = new WHONetRow("NAT-1", "Ada, \"A\"\n", "Lovelace", "F", "1990-01-01", "2026-07-10", "LAB-001",
                "2026-07-09", "Blood", "CIP", "eco", "S", "MIC", "", "");
        when(datasetService.compile(query)).thenReturn(new MicroWhonetDataset(preview, List.of(row)));

        MicroWhonetExportResult result = service.generateMicrobiologyExport(query, " authenticated-user ");

        String csv = new String(result.getContent(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\"NATIONAL_ID\",\"FIRST_NAME\""));
        assertTrue(csv.contains("\"Ada, \"\"A\"\"\n\""));
        assertTrue(csv.contains("\"CIP\",\"eco\",\"S\",\"MIC\""));
        assertEquals("WHONET_2026-07-01_to_2026-07-31.csv", result.getFileName());

        ArgumentCaptor<MicroWhonetExportRun> audit = ArgumentCaptor.forClass(MicroWhonetExportRun.class);
        verify(exportRunDAO).insert(audit.capture());
        assertEquals("authenticated-user", audit.getValue().getGeneratedBy());
        assertEquals(1, audit.getValue().getRowCount());
        assertEquals(64, audit.getValue().getContentSha256().length());
    }

    @Test
    public void generationRejectsPreviewWithNoExportableRows() {
        MicroWhonetExportQueryForm query = query();
        when(datasetService.compile(query)).thenReturn(new MicroWhonetDataset(preview(false), List.of()));

        try {
            service.generateMicrobiologyExport(query, "authenticated-user");
            fail("Expected export to be blocked");
        } catch (MicroWhonetExportBlockedException expected) {
            assertEquals("No valid WHONET rows remain after validation", expected.getMessage());
        }

        verify(exportRunDAO, never()).insert(any(MicroWhonetExportRun.class));
    }

    @Test
    public void exportResultDefensivelyCopiesContent() {
        byte[] source = new byte[] { 1, 2, 3 };
        MicroWhonetExportResult result = new MicroWhonetExportResult("WHONET.csv", source);

        source[0] = 9;
        byte[] returned = result.getContent();
        returned[1] = 8;

        assertEquals("WHONET.csv", result.getFileName());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result.getContent());
    }

    private MicroWhonetExportQueryForm query() {
        MicroWhonetExportQueryForm query = new MicroWhonetExportQueryForm();
        query.from = "2026-07-01";
        query.to = "2026-07-31";
        query.significance = List.of("CLINICALLY_SIGNIFICANT");
        query.dedup = "FIRST_ISOLATE_7_DAY";
        return query;
    }

    private MicroWhonetPreviewForm preview(boolean canGenerate) {
        MicroWhonetPreviewForm preview = new MicroWhonetPreviewForm();
        preview.from = "2026-07-01";
        preview.to = "2026-07-31";
        preview.significance = "CLINICALLY_SIGNIFICANT";
        preview.dedup = "FIRST_ISOLATE_7_DAY";
        preview.totalCases = 1;
        preview.exportableIsolates = canGenerate ? 1 : 0;
        preview.exportedRows = canGenerate ? 1 : 0;
        preview.canGenerate = canGenerate;
        return preview;
    }
}
