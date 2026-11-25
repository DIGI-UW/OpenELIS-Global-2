package org.openelisglobal.resultrun.form;

/**
 * Another run a test line points to (OGC-1200 FR-C5, FR-A3): the run this one
 * retests, a run it may retest, or the run that superseded it, with how many of
 * the retested samples the link covers out of all that were set aside.
 */
public record RunLink(Long runId, String label, int count, int total) {
}
