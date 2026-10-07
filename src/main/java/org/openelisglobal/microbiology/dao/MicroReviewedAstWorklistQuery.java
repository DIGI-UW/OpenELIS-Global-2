package org.openelisglobal.microbiology.dao;

public record MicroReviewedAstWorklistQuery(String testSectionId, String stage, String urgency, String due,
        String search, String sort, int offset, int limit, boolean allUnits, java.util.Set<String> permittedUnitIds) {
}
