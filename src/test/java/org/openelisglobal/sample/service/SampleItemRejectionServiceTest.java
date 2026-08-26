package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public class SampleItemRejectionServiceTest {

    @Test
    public void rejectsSampleItemAndItsAnalysesWithAuditedReason() {
        SampleItemService sampleItemService = mock(SampleItemService.class);
        AnalysisService analysisService = mock(AnalysisService.class);
        IStatusService statusService = mock(IStatusService.class);
        NoteService noteService = mock(NoteService.class);
        SampleItemRejectionService service = new SampleItemRejectionServiceImpl(sampleItemService, analysisService,
                statusService, noteService);
        SampleItem item = new SampleItem();
        item.setId("1001");
        Analysis first = new Analysis();
        first.setId("analysis-1");
        Analysis second = new Analysis();
        second.setId("analysis-2");
        Note note = new Note();
        when(sampleItemService.get("1001")).thenReturn(item);
        when(analysisService.getAnalysesBySampleItem(item)).thenReturn(List.of(first, second));
        when(statusService.getStatusID(SampleStatus.SampleRejected)).thenReturn("sample-rejected");
        when(statusService.getStatusID(AnalysisStatus.SampleRejected)).thenReturn("analysis-rejected");
        when(noteService.createSavableNote(item, NoteType.REJECTION_REASON, "Specimen lost", "Sample rejection", "17"))
                .thenReturn(note);

        service.reject("1001", "Specimen lost", "17");

        assertTrue(item.isRejected());
        assertEquals("sample-rejected", item.getStatusId());
        assertEquals("17", item.getSysUserId());
        assertEquals("analysis-rejected", first.getStatusId());
        assertEquals("analysis-rejected", second.getStatusId());
        assertEquals("17", first.getSysUserId());
        verify(sampleItemService).update(item);
        verify(analysisService).update(first);
        verify(analysisService).update(second);
        verify(noteService).insert(note);
    }
}
