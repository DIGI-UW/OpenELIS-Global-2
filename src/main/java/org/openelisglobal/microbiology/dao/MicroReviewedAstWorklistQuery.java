package org.openelisglobal.microbiology.dao;

public record MicroReviewedAstWorklistQuery(String workflow, String stage, String urgency, String due, String search,
        String sort, int offset, int limit) {
}
