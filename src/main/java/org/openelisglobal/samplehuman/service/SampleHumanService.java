package org.openelisglobal.samplehuman.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleHumanService extends BaseObjectService<SampleHuman, String> {
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    void getData(SampleHuman sampleHuman);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    Provider getProviderForSample(Sample sample);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientForSample(Sample sample);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<Sample> getSamplesForPatient(String patientID);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    SampleHuman getDataBySample(SampleHuman sampleHuman);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<String> getAllPatientIdsWithSampleEntered();

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<String> getAllPatientIdsWithSampleEnteredMissingFhirUuid();

}
