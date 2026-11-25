package org.openelisglobal.batchworkplan.service;

/** A distinct test in a batch, with what a QC entry for it needs. */
public record BatchTestRef(String testId, String testName, String testSectionId) {
}
