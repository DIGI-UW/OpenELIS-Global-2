package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * One unresolved record of a run's Exceptions block (OGC-1200 FR-E4): the
 * values that share a cause, so they are resolved together, and the latest hold
 * someone put on them.
 *
 * @param mapping where Map now opens Analyzer Types and Mapping, for an
 *                unmapped code whose analyzer and code are known
 */
public record ResultRunException(String key, ResultRunExceptionKind kind, String accessionNumber, String testId,
        String testName, String code, String value, String issue, List<String> rowIds, MappingTarget mapping,
        Hold hold) {

    public record MappingTarget(String analyzerId, String focusTest, String focusValue) {
    }

    public record Hold(String reason, String heldBy, String heldAt) {
    }
}
