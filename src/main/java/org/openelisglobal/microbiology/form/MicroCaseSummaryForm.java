package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCaseSummaryForm {
    public String domain;
    public String siteName;
    public String siteCode;
    public String id;
    public String accessionNumber;
    public String patientId;
    public String patientName;
    public Timestamp birthDate;
    public String gender;
    public String labUnitId;
    public String labUnit;
    public String specimenType;
    public String status;
    public String stage;
    public String priority;
    public Timestamp createdAt;
    public String caseStatus;
    public java.util.List<String> statusTags = new java.util.ArrayList<>();
    public boolean stat;
}
