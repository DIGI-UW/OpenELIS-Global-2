package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * One unresolved record of a run's Exceptions block (OGC-1200 FR-E4): the
 * values that share a cause, so they are resolved together, and the latest hold
 * someone put on this record.
 *
 * @param componentId     the part of the test the first value fills, so the
 *                        order search says whether that part still waits
 * @param reportedPatient the patient the analyzer reported with the values, to
 *                        compare with the order a value is matched to
 * @param mapping         where Map now opens Analyzer Types and Mapping, for an
 *                        unmapped code whose analyzer and code are known
 */
public record ResultRunException(String key, ResultRunExceptionKind kind, String accessionNumber, String testId,
        String componentId, String testName, String code, String value, String issue, String reportedPatient,
        List<String> rowIds, MappingTarget mapping, Hold hold) {

    public record MappingTarget(String analyzerId, String focusTest, String focusValue) {
    }

    public record Hold(String reason, String heldBy, String heldAt) {
    }
}
