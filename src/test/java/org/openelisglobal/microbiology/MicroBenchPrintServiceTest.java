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
        var page = new MicroBenchPageForm();
        page.total = 1;
        page.rows = List.of(row);
        when(bench.search(any(), eq("reader"))).thenReturn(page);
        var request = new MicroBenchPrintForm();
        request.labUnitId = "unit";
        request.caseIds = List.of("case");
        var printed = service.print(request, "reader");
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
