package org.openelisglobal.resultrun.form;

/**
 * An order a run exception can be matched to (OGC-1200 FR-E4), with what the
 * technician checks before choosing it: whose order it is and whether the
 * value's test is ordered on it.
 */
public record RunOrderMatch(String accessionNumber, String patientName, String receivedDate, boolean testOrdered) {
}
