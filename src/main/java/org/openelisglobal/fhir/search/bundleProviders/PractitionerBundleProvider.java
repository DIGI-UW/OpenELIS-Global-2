package org.openelisglobal.fhir.search.bundleProviders;

import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.search.service.dao.PractitionerSearchDao;

/**
 * Lazy DAO-backed bundle provider for Practitioner searches.
 */
public class PractitionerBundleProvider extends BaseFhirBundleProvider<Provider, Practitioner> {

    private final PractitionerSearchParams searchParams;

    private final PractitionerSearchDao practitionerSearchDao;

    private final FhirTransformService fhirTransformService;

    public PractitionerBundleProvider(PractitionerSearchParams searchParams,
            PractitionerSearchDao practitionerSearchDao, FhirTransformService fhirTransformService) {

        this.searchParams = searchParams;

        this.practitionerSearchDao = Objects.requireNonNull(practitionerSearchDao,
                "PractitionerSearchDao must not be null");

        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    /**
     * Loads only the Provider records requested by the current HAPI FHIR page.
     */
    @Override
    protected List<Provider> loadEntities(int offset, int pageSize) {

        return practitionerSearchDao.search(searchParams, offset, pageSize);
    }

    /**
     * Counts all Provider records matching the Practitioner search parameters.
     */
    @Override
    protected long countEntities() {

        return practitionerSearchDao.count(searchParams);
    }

    /**
     * Converts an OpenELIS Provider into a FHIR Practitioner.
     */
    @Override
    protected Practitioner transformEntity(Provider provider) {

        return fhirTransformService.transformProviderToPractitioner(provider);
    }
}
