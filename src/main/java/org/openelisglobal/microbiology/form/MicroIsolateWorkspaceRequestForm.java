package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.referral.action.beanitems.ReferralItem;

public class MicroIsolateWorkspaceRequestForm {
    public String cultureId, sampleItemId, sampleTypeId, purpose;
    public String gramStain, colonyMorphology, significance;
    public String organismId, identificationStatus, identificationMethod, reason, version;
    public BigDecimal confidence;
    public Timestamp identifiedAt;
    public String sendingOrganizationId, reportedOrganismId, reportedOrganismText;
    public String testId;
    public ReferralItem referral;
}
