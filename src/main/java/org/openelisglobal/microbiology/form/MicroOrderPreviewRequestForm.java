package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroOrderPreviewRequestForm {
    public List<Specimen> specimens = new ArrayList<>();

    public static class Specimen {
        public String sampleTypeId;
        public Integer cultureSetNumber;
        public String container;
        public String bodySite;
        public String collectionDate;
        public String collectionTime;
        public List<String> testIds = new ArrayList<>();
    }
}
