package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.*;
import org.openelisglobal.common.util.IdValuePair;

public class MicroBenchPageForm {
    public List<Row> rows = new ArrayList<>();
    public List<IdValuePair> labUnits = new ArrayList<>();
    public long total;
    public int page, pageSize;

    public static class Row {
        public String id, caseId, cultureId, accessionNumber, patientName, labUnitId, labUnit, specimenType, priority,
                stage, origin;
        public MicroCultureForm culture;
        public List<String> attention = new ArrayList<>();
        public List<IdValuePair> cultureTests = new ArrayList<>();
        public String sampleTypeId, sampleItemId;
        public boolean canWrite;
        public Timestamp dueAt;
    }
}
