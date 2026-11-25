package org.openelisglobal.analyzerresults.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerHeldResultServiceImpl implements AnalyzerHeldResultService {

    private final AnalyzerResultsService analyzerResultsService;

    public AnalyzerHeldResultServiceImpl(AnalyzerResultsService analyzerResultsService) {
        this.analyzerResultsService = analyzerResultsService;
    }

    @Override
    @Transactional
    public void dismiss(String stagedResultId, String actor) {
        AnalyzerResults held = analyzerResultsService.get(stagedResultId);
        // Held for its mapping or for placement: either way it carries the reason.
        if (held.getImportIssueReason() == null) {
            throw new IllegalStateException("Only a held result can be dismissed");
        }
        for (AnalyzerResults row : heldRowsOfItsTest(held)) {
            analyzerResultsService.delete(row.getId(), actor);
        }
    }

    /**
     * A test is reviewed on its main row; the held rows of its components, and any
     * held copy of it, on the same tube leave review with it (OGC-1662: the
     * component rows stayed and the test came straight back).
     */
    private List<AnalyzerResults> heldRowsOfItsTest(AnalyzerResults held) {
        if (held.getTestId() == null || held.getAnalyzerId() == null || held.getAccessionNumber() == null) {
            return List.of(held);
        }
        List<AnalyzerResults> rows = analyzerResultsService.getAllMatching(Map.of("analyzerId", held.getAnalyzerId(),
                "accessionNumber", held.getAccessionNumber(), "testId", held.getTestId()));
        List<AnalyzerResults> ofTheTest = rows.stream()
                .filter(row -> Objects.equals(row.getInstrumentSpecimenId(), held.getInstrumentSpecimenId()))
                .filter(row -> row.getImportIssueReason() != null).toList();
        return ofTheTest.isEmpty() ? List.of(held) : ofTheTest;
    }
}
