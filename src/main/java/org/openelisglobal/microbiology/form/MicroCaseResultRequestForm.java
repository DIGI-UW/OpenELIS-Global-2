package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroCaseResultRequestForm {
    public String version;
    public String note;
    public List<ResultComponent> components;

    public static class ResultComponent {
        public String componentId;
        public String value;
        public String multiSelectResultValues;
        public boolean criticalAcknowledged;
        public boolean invalidResultConfirmed;
    }
}
