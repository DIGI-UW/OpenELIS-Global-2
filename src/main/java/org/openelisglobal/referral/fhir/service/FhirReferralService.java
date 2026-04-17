package org.openelisglobal.referral.fhir.service;

import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.referral.valueholder.Referral;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FhirReferralService {

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    void setReferralResult(ReferralResultsImportObjects resultsImport);

    @PreAuthorize("hasAuthority('PRIV_REFERRAL_MANAGE')")
    Bundle referAnalysisesToOrganization(Referral referral) throws FhirLocalPersistingException;

    // Bundle cancelReferralToOrganization(String organizationId, String sampleId,
    // List<String>
    // analysisIds)
    // throws FhirLocalPersistingException;

}
