package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

public class MicroPatientOriginOptions {

    private final List<MicroPatientOrigin> options;
    private final String defaultCode;

    public MicroPatientOriginOptions(List<MicroPatientOrigin> options, String defaultCode) {
        this.options = options;
        this.defaultCode = defaultCode;
    }

    public List<MicroPatientOrigin> getOptions() {
        return options;
    }

    public String getDefaultCode() {
        return defaultCode;
    }
}
