package org.openelisglobal.eqa.service;

/**
 * A value a participant reported for one row of a cycle's intake: the test, the
 * panel sample it answers when the panel carries one, and the value as reported
 * (a number or a word). A null {@code panelSampleId} is accepted for a test
 * with a single row, which is every test on a one-sample-per-test panel.
 */
public record EQAIntakeValue(Long testId, Long panelSampleId, String value) {
}
