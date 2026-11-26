package org.openelisglobal.program.valueholder;

/**
 * One earlier case in the prior results panel. The conclusion is sent only for
 * a signed-out case whose conclusion state is REPORTED; otherwise it is null.
 */
public record PriorResultRow(String bench, Integer caseId, String labNumber, String requestDate, String specimen,
        String conclusionState, String conclusion) {
}
