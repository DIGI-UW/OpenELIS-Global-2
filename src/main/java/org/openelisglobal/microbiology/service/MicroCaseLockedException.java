package org.openelisglobal.microbiology.service;

public class MicroCaseLockedException extends IllegalStateException {

    public MicroCaseLockedException(String message) {
        super(message);
    }
}
