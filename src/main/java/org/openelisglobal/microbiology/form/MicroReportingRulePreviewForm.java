package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroReportingRulePreviewForm {
    public String organismId;
    public String sampleTypeId;
    public String clinicalContext;
    public List<Reading> readings = List.of();

    public static class Reading {
        public String antibioticId;
        public String interpretation;
    }
}
