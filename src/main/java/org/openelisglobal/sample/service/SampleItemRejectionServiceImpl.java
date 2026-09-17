package org.openelisglobal.sample.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleItemRejectionServiceImpl implements SampleItemRejectionService {

    private static final String REJECTION_SUBJECT = "Sample rejection";

    private final SampleItemService sampleItemService;
    private final AnalysisService analysisService;
    private final IStatusService statusService;
    private final NoteService noteService;

    public SampleItemRejectionServiceImpl(SampleItemService sampleItemService, AnalysisService analysisService,
            IStatusService statusService, NoteService noteService) {
        this.sampleItemService = sampleItemService;
        this.analysisService = analysisService;
        this.statusService = statusService;
        this.noteService = noteService;
    }

    @Override
    @Transactional
    public void reject(String sampleItemId, String reason, String authenticatedUserId) {
        requireText(sampleItemId, "sampleItemId");
        requireText(reason, "reason");
        requireText(authenticatedUserId, "authenticatedUserId");
        SampleItem item = sampleItemService.get(sampleItemId);
        if (item == null) {
            throw new IllegalArgumentException("Sample item not found");
        }

        item.setRejected(true);
        item.setStatusId(statusService.getStatusID(SampleStatus.SampleRejected));
        item.setSysUserId(authenticatedUserId);
        sampleItemService.update(item);

        String rejectedStatus = statusService.getStatusID(AnalysisStatus.SampleRejected);
        for (Analysis analysis : analysisService.getAnalysesBySampleItem(item)) {
            if (terminal(analysis)) {
                continue;
            }
            analysis.setStatusId(rejectedStatus);
            analysis.setSysUserId(authenticatedUserId);
            analysisService.update(analysis);
        }

        Note note = noteService.createSavableNote(item, NoteType.REJECTION_REASON, reason.trim(), REJECTION_SUBJECT,
                authenticatedUserId);
        noteService.insert(note);
    }

    private boolean terminal(Analysis analysis) {
        return statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                || statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)
                || statusService.matches(analysis.getStatusId(), AnalysisStatus.SampleRejected);
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
