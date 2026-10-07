package org.openelisglobal.microbiology.service;

import java.util.List;

public class MicroCaseCancellationRequiredException extends IllegalArgumentException {
    public record AffectedCase(String caseId, String labUnit, boolean hasResults) {
    }

    private final List<AffectedCase> cases;

    public MicroCaseCancellationRequiredException(List<AffectedCase> cases) {
        super("Confirm cancellation of the last microbiology test on these cases");
        this.cases = List.copyOf(cases);
    }

    public List<AffectedCase> getCases() {
        return cases;
    }
}
