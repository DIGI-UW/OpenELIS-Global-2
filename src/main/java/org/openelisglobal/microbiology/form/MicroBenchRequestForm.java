package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroBenchRequestForm {
    public String action, sampleTypeId, cultureTestId;
    public List<Selection> cases;
    public MicroCultureRequestForm culture;

    public static class Selection {
        public String caseId, sampleItemId;
        public List<String> cultureIds;
    }
}
