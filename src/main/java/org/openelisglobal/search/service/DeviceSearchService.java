package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.DeviceBundleProvider;
import org.openelisglobal.fhir.search.searchparams.DeviceSearchParams;
import org.openelisglobal.search.dao.DeviceSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeviceSearchService {

    private final DeviceSearchDao deviceSearchDao;
    private final FhirTransformService fhirTransformService;

    public DeviceSearchService(DeviceSearchDao deviceSearchDao, FhirTransformService fhirTransformService) {
        this.deviceSearchDao = deviceSearchDao;
        this.fhirTransformService = fhirTransformService;
    }

    public IBundleProvider searchDevices(DeviceSearchParams params) {
        return new DeviceBundleProvider(params, deviceSearchDao, fhirTransformService);
    }
}
