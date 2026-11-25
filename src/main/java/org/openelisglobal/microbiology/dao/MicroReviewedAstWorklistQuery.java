package org.openelisglobal.microbiology.dao;

public record MicroReviewedAstWorklistQuery(String stage, String urgency, String due, String search, String sort,
        int offset, int limit, java.util.Set<String> permittedLabUnitIds) {
    public MicroReviewedAstWorklistQuery(String stage, String urgency, String due, String search, String sort,
            int offset, int limit) {
        this(stage, urgency, due, search, sort, offset, limit, null);
    }
}
