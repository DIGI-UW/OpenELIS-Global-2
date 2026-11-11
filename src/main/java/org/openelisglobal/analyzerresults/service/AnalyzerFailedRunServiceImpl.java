package org.openelisglobal.analyzerresults.service;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl;
import org.openelisglobal.note.valueholder.Note;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerFailedRunServiceImpl implements AnalyzerFailedRunService {

    private static final String SUBJECT = "Analyzer run failed";

    private final AnalyzerResultsService analyzerResultsService;
    private final AnalyzerResultPlacementService placementService;
    private final AnalysisService analysisService;
    private final NoteService noteService;

    public AnalyzerFailedRunServiceImpl(AnalyzerResultsService analyzerResultsService,
            AnalyzerResultPlacementService placementService, AnalysisService analysisService, NoteService noteService) {
        this.analyzerResultsService = analyzerResultsService;
        this.placementService = placementService;
        this.analysisService = analysisService;
        this.noteService = noteService;
    }

    @Override
    @Transactional
    public String dismissAsFailedRun(String stagedResultId, String actor) {
        AnalyzerResults run = analyzerResultsService.get(stagedResultId);
        if (!AnalyzerResults.IMPORT_ISSUE_RUN_FAILED.equals(run.getImportIssueReason())) {
            throw new IllegalStateException("Only a failed run can be dismissed as a failed run");
        }
        if (run.getTestId() == null) {
            throw new IllegalStateException("The failed run has no test to record the failure on");
        }
        String specimenId = run.getInstrumentSpecimenId() != null ? run.getInstrumentSpecimenId()
                : run.getAccessionNumber();
        AnalyzerResultPlacement placement = placementService.place(specimenId, run.getTestId());
        if (placement.analyses().size() != 1) {
            throw new IllegalStateException("The failed run matches no single test on an order");
        }
        Analysis analysis = analysisService.get(placement.analyses().get(0).analysisId());
        String reported = run.getRawResultValue() != null ? run.getRawResultValue() : run.getResult();
        String text = "Analyzer run failed (" + reported + ")"
                + (run.getInstrumentNote() == null ? "" : ": " + run.getInstrumentNote());
        Note note = noteService.createSavableNote(analysis, NoteServiceImpl.NoteType.INTERNAL, text, SUBJECT, actor);
        noteService.insert(note);
        analyzerResultsService.delete(run.getId(), actor);
        return analysis.getId();
    }
}
