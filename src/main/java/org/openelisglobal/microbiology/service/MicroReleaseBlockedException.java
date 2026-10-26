package org.openelisglobal.microbiology.service;

import java.util.List;

public class MicroReleaseBlockedException extends IllegalStateException {

    private final List<String> blockers;

    public MicroReleaseBlockedException(String message, List<String> blockers) {
        super(message);
        this.blockers = blockers;
    }

    public List<String> getBlockers() {
        return blockers;
    }
}
