package org.openelisglobal.resultrun.form;

import org.openelisglobal.resultrun.valueholder.RunQcItemKind;

/**
 * A lab blank or lab duplicate recorded on a run for one test (OGC-1200 FR-C10,
 * FR-C11): its numeric value or its outcome, and for a duplicate the analysis
 * of the run's sample it repeats.
 */
public record RunQcItemForm(String testId, RunQcItemKind kind, String value, String outcome, String parentAnalysisId) {
}
