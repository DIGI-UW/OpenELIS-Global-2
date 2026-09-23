package org.openelisglobal.search.service;

import java.util.List;
import java.util.Objects;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SampleHumanSearchDao extends BaseFhirDao {

    public SampleHumanSearchDao(FhirPropertyResolver propertyResolver) {

        super(propertyResolver);
    }

    public List<SampleHuman> findByProviders(List<Provider> providers) {

        if (providers == null || providers.isEmpty()) {
            return List.of();
        }

        List<String> providerIds = providers.stream().filter(Objects::nonNull).map(Provider::getId)
                .filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).distinct().toList();

        if (providerIds.isEmpty()) {
            return List.of();
        }

        FhirCriteriaContext<SampleHuman, SampleHuman> context = createCriteriaContext(SampleHuman.class);

        context.addPredicate(context.getRoot().get(FhirConstants.PROVIDER_ID).in(providerIds));

        context.distinct(true);

        return list(context);
    }

    /** sample_human rows for the given patients, keyed off Patient.id. */
    public List<SampleHuman> findByPatients(List<org.openelisglobal.patient.valueholder.Patient> patients) {

        if (patients == null || patients.isEmpty()) {
            return List.of();
        }

        List<String> patientIds = patients.stream().filter(Objects::nonNull)
                .map(org.openelisglobal.patient.valueholder.Patient::getId).filter(Objects::nonNull).map(String::trim)
                .filter(id -> !id.isEmpty()).distinct().toList();

        if (patientIds.isEmpty()) {
            return List.of();
        }

        FhirCriteriaContext<SampleHuman, SampleHuman> context = createCriteriaContext(SampleHuman.class);

        context.addPredicate(context.getRoot().get(FhirConstants.PATIENT_ID).in(patientIds));

        context.distinct(true);

        return list(context);
    }

    /** sample_human rows for the given Sample primary keys. */
    public List<SampleHuman> findBySampleIds(List<String> sampleIds) {

        List<String> ids = sampleIds == null ? List.of()
                : sampleIds.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).distinct()
                        .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        FhirCriteriaContext<SampleHuman, SampleHuman> context = createCriteriaContext(SampleHuman.class);

        context.addPredicate(context.getRoot().get(FhirConstants.SAMPLE_ID).in(ids));

        context.distinct(true);

        return list(context);
    }
}