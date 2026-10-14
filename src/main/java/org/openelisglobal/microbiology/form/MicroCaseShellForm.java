package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class MicroCaseShellForm extends MicroCaseSummaryForm {
    public List<MicroCaseSpecimenForm> samples = new ArrayList<>();
    public List<MicroCaseSpecimenForm> pendingSamples = new ArrayList<>();
    public List<MicroCaseSummaryForm> relatedCases = new ArrayList<>();
    public List<IdValuePair> transferLabUnits = new ArrayList<>();
    public String orderId;
    public String programId;
    public List<IdValuePair> programs = new ArrayList<>();
    public String culturePurpose;
    public List<String> purposeOptions = new ArrayList<>();
    public String siteId;
    public String siteLocation;
    public String latitude;
    public String longitude;
    public String requesterName;
    public String requesterPhone;
    public Integer replicateCount;
    public int orderCaseCount;
    public String finalReleaseState;
    public boolean amendmentOpen;
    public boolean referred;
    public boolean canWrite;
    public boolean canValidate;
    public boolean readOnlyAccess;
}
