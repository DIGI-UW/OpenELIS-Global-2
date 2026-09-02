package org.openelisglobal.fhir.search.bundleProviders;

import java.util.List;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;

public class ServiceRequestBundleProvider extends BaseFhirBundleProvider<Analysis, ServiceRequest> {
    private ServiceRequestSearchDao serviceRequestSearchDao;
    private ServiceRequestSearchParams searchParams;
    private FhirTransformService fhirTransformService;

    @Override
    protected List<Analysis> loadEntities(int offset, int pageSize) {
        return serviceRequestSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return serviceRequestSearchDao.count(searchParams);
    }

    @Override
    protected ServiceRequest transformEntity(Analysis entity) {
        return fhirTransformService.transformToServiceRequest(entity.getId());
    }

}
