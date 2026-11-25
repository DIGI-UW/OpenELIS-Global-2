package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * A stage transition was asked of a case that no longer stands where the
 * request said it did: another user moved it first, so the page that sent the
 * request is out of date.
 */
public class PathologyStageConflictException extends RuntimeException {

    private final PathologyStatus storedStage;

    public PathologyStageConflictException(Integer pathologySampleId, PathologyStatus storedStage,
            PathologyStatus expectedStage) {
        super("pathology case " + pathologySampleId + " stands at " + storedStage + ", not " + expectedStage);
        this.storedStage = storedStage;
    }

    /** The stage the case actually stands at. */
    public PathologyStatus getStoredStage() {
        return storedStage;
    }
}
