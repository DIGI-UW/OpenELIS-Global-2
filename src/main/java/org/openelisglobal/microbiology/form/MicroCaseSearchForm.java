package org.openelisglobal.microbiology.form;

public class MicroCaseSearchForm {
    public boolean migrationReview;
    public String status;
    public String from;
    public String to;
    public String q;
    public String patientId;
    public String accessionNumber;
    public String labUnitId;
    public java.util.List<String> labUnitIds;
    public String sort = "newest";
    public int page = 1;
    public int pageSize = 20;
}
