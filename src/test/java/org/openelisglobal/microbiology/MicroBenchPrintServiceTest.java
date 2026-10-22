package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.workplan.dao.WorkplanPrintRecordDAO;
import org.openelisglobal.workplan.valueholder.WorkplanPrintRecord;

public class MicroBenchPrintServiceTest {
    private Object previousMessages;

    @org.junit.Before
    public void messages() {
        previousMessages = org.springframework.test.util.ReflectionTestUtils
                .getField(org.openelisglobal.internationalization.MessageUtil.class, "instance");
        var source = new org.springframework.context.support.ResourceBundleMessageSource();
        source.setBasename("languages/message");
        source.setDefaultEncoding("UTF-8");
        org.openelisglobal.internationalization.MessageUtil.setMessageSource(source);
    }

    @org.junit.After
    public void restoreMessages() {
        org.springframework.test.util.ReflectionTestUtils
                .setField(org.openelisglobal.internationalization.MessageUtil.class, "instance", previousMessages);
    }

    @Test
    public void selectedCasesReadOnlyRequestedIdsAndRejectChangedOwnership() {
        var workspace = mock(MicroCaseWorkspaceService.class);
        var access = mock(MicrobiologyCaseAccessService.class);
        var service = new MicroBenchServiceImpl(workspace, mock(MicroCultureService.class),
                mock(org.openelisglobal.microbiology.dao.MicroIncomingItemDAO.class),
                mock(org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO.class),
                mock(org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO.class), access,
                mock(MicroBenchCaseService.class));
        when(access.canReadLabUnit("reader", "unit")).thenReturn(true);
        var shell = new MicroCaseShellForm();
        shell.id = "case";
        shell.labUnitId = "unit";
        when(workspace.get("case", "reader")).thenReturn(shell);
        assertEquals(List.of("case"),
                service.selectedCases("unit", List.of("case"), "reader").stream().map(r -> r.caseId).toList());
        verify(workspace).get("case", "reader");
        verify(workspace, never()).search(any(), anyString());
        shell.labUnitId = "transferred";
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.selectedCases("unit", List.of("case"), "reader"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.selectedCases("unit", List.of("case"), "outsider"));
    }

    @Test
    public void batchUsesTheCultureAnalysisSpecimenAndOnlyItsTests() {
        var workspace = mock(MicroCaseWorkspaceService.class);
        var access = mock(MicrobiologyCaseAccessService.class);
        var analyses = mock(org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO.class);
        var service = new MicroBenchServiceImpl(workspace, mock(MicroCultureService.class),
                mock(org.openelisglobal.microbiology.dao.MicroIncomingItemDAO.class), analyses,
                mock(org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO.class), access,
                mock(MicroBenchCaseService.class));
        when(access.canReadLabUnit("writer", "unit")).thenReturn(true);
        var shell = new MicroCaseShellForm();
        shell.id = "case";
        shell.labUnitId = "unit";
        for (String id : List.of("unrelated", "culture-source", "another-source")) {
            var sample = new MicroCaseSpecimenForm();
            sample.sampleItemId = id;
            sample.sampleTypeId = id + "-type";
            shell.samples.add(sample);
        }
        when(workspace.get("case", "writer")).thenReturn(shell);
        var selected = cultureAnalysis("culture-source", "culture-test");
        var other = cultureAnalysis("another-source", "other-test");
        when(analyses.getAnalyses("case")).thenReturn(List.of(selected, other));
        var row = service.selectedCases("unit", List.of("case"), "writer").get(0);
        assertEquals("culture-source", row.sampleItemId);
        assertEquals("culture-source-type", row.sampleTypeId);
        assertEquals(List.of("culture-test"), row.cultureTests.stream().map(t -> t.getId()).toList());
    }

    private org.openelisglobal.analysis.valueholder.Analysis cultureAnalysis(String sourceId, String testId) {
        var sample = new org.openelisglobal.sampleitem.valueholder.SampleItem();
        sample.setId(sourceId);
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        test.setMicrobiologyCaseRole("CULTURE");
        var analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setSampleItem(sample);
        analysis.setTest(test);
        return analysis;
    }

    @Test
    public void openingSheetReplaysBytesWithoutReadingChangedCases() throws Exception {
        var bench = mock(MicroBenchService.class);
        var cultures = mock(MicroCultureService.class);
        var records = mock(WorkplanPrintRecordDAO.class);
        var access = mock(MicrobiologyCaseAccessService.class);
        var service = new MicroBenchPrintServiceImpl(bench, cultures, records, access, new ObjectMapper());
        when(access.canReadLabUnit("reader", "unit")).thenReturn(true);
        var row = new MicroBenchPageForm.Row();
        row.caseId = "case";
        row.labUnit = "Culture unit";
        row.accessionNumber = "PRINT-001";
        row.patientName = "Test Patient";
        row.cultureTests = List.of();
        when(bench.selectedCases("unit", List.of("case"), "reader")).thenReturn(List.of(row));
        var request = new MicroBenchPrintForm();
        request.labUnitId = "unit";
        request.caseIds = List.of("case");
        var printed = service.print(request, "reader");
        verify(bench).selectedCases("unit", List.of("case"), "reader");
        verify(bench, never()).search(any(), anyString());
        var captured = ArgumentCaptor.forClass(WorkplanPrintRecord.class);
        verify(records).insert(captured.capture());
        when(records.get(printed.id())).thenReturn(Optional.of(captured.getValue()));
        byte[] original = service.open(printed.id(), "reader");
        assertEquals("%PDF", new String(original, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        row.accessionNumber = "CHANGED";
        reset(bench, cultures);
        assertArrayEquals(original, service.open(printed.id(), "reader"));
        verifyZeroInteractions(bench, cultures);
        try {
            service.open(printed.id(), "outsider");
            fail("Sheet must retain lab unit access boundary");
        } catch (org.springframework.security.access.AccessDeniedException expected) {
        }
        java.nio.file.Files.write(java.nio.file.Path.of("target/m7-bench-sheet.pdf"), original);
    }
}
