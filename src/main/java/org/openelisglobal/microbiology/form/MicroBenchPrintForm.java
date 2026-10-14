package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroBenchPrintForm {
    public String labUnitId;
    public List<String> caseIds;
    public List<String> cultureIds;

    public record SavedSheet(String id, String labUnitId, java.sql.Timestamp printedAt) {
    }
}
