package org.openelisglobal.referral.fhir.form;

import java.util.List;
import org.openelisglobal.referral.action.beanitems.ReferralItem;

public class FhirReferralItem extends ReferralItem {

    private String organizationId;

    private String sampleId;

    private List<String> analysisIds;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public List<String> getAnalysisIds() {
        return analysisIds;
    }

    public void setAnalysisIds(List<String> analysisIds) {
        this.analysisIds = analysisIds;
    }
}
