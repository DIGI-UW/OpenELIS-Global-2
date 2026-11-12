package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroReportingRulePreviewResultForm {
    public String defaultPanelName;
    public List<Row> rows = new ArrayList<>();

    public static class Row {
        public String antibioticId;
        public String antibioticName;
        public String measured;
        public String interpretation;
        public String expertRule;
        public String reportBehavior;
        public String reportRuleId;
        public String note;
        public boolean reported;
    }
}
