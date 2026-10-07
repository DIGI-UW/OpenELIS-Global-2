package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroCaseDetailForm {

    public String id;
    public String sampleId;
    public String testSectionId;
    public String testSectionName;
    public String programId;
    public boolean migrationReviewRequired;
    public boolean canEnterResults;
    public boolean canValidateResults;
    public String patientId;
    public String patientName;
    public String accessionNumber;
    public String specimenType;
    public String requestingLocation;
    public String stage;
    public String priority;
    public Timestamp createdAt;
    public String createdBy;
    public Timestamp closedAt;
    public String closedBy;
    public String finalReleaseState;
    public String lastActivityBy;
    public Timestamp lastActivityAt;
    public int nonconformanceCount;
    public List<MicroCultureSetWarningForm> setWarnings = new ArrayList<>();
    public List<MicroCaseSpecimenForm> specimens = new ArrayList<>();
    public List<MicroCaseRequestedSpecimenForm> requestedSpecimens = new ArrayList<>();
    public MicroCaseOrderDetailForm orderDetail;
    public List<MicroCaseActivityForm> activities = new ArrayList<>();
    public List<MicroIsolateForm> isolates = new ArrayList<>();
    public List<MicroCaseLookupForm> siblingCases = new ArrayList<>();
}
