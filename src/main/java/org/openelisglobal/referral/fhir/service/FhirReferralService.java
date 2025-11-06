package org.openelisglobal.referral.fhir.service;

import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.referral.valueholder.Referral;

public interface FhirReferralService {

    void setReferralResult(ReferralResultsImportObjects resultsImport);

    Bundle referAnalysisesToOrganization(Referral referral) throws FhirLocalPersistingException;

    // Bundle cancelReferralToOrganization(String organizationId, String sampleId,
    // List<String>
    // analysisIds)
    // throws FhirLocalPersistingException;

}
