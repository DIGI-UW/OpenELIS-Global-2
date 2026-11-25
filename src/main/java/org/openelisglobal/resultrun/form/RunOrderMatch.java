package org.openelisglobal.resultrun.form;

/**
 * An order a run exception can be matched to (OGC-1200 FR-E4), with what the
 * technician checks before choosing it: whose order it is and where the value's
 * test stands on it.
 */
public record RunOrderMatch(String accessionNumber, String patientName, String receivedDate, RunOrderState test) {
}
