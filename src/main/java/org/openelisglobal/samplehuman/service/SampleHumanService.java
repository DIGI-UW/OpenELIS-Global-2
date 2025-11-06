package org.openelisglobal.samplehuman.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;

public interface SampleHumanService extends BaseObjectService<SampleHuman, String> {
    void getData(SampleHuman sampleHuman);

    Provider getProviderForSample(Sample sample);

    Patient getPatientForSample(Sample sample);

    List<Sample> getSamplesForPatient(String patientID);

    SampleHuman getDataBySample(SampleHuman sampleHuman);

    List<Patient> getAllPatientsWithSampleEntered();

    List<Patient> getAllPatientsWithSampleEnteredMissingFhirUuid();
}
