package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Device;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.DeviceSearchParams;
import org.openelisglobal.search.dao.DeviceSearchDao;

/** Pages OpenELIS analyzers as FHIR Device resources. */
public class DeviceBundleProvider extends BaseFhirBundleProvider<Analyzer, Device> {

    private final DeviceSearchParams searchParams;
    private final DeviceSearchDao deviceSearchDao;
    private final FhirTransformService fhirTransformService;

    public DeviceBundleProvider(DeviceSearchParams searchParams, DeviceSearchDao deviceSearchDao,
            FhirTransformService fhirTransformService) {
        this.searchParams = Objects.requireNonNull(searchParams, "DeviceSearchParams must not be null");
        this.deviceSearchDao = Objects.requireNonNull(deviceSearchDao, "DeviceSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<Analyzer> loadEntities(int offset, int pageSize) {
        return deviceSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return deviceSearchDao.count(searchParams);
    }

    @Override
    protected Device transformEntity(Analyzer analyzer) {
        Device device = fhirTransformService.transformAnalyzerToDevice(analyzer);
        if (device != null) {
            ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(device, BundleEntrySearchModeEnum.MATCH);
        }
        return device;
    }

    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {
        return super.getResources(fromIndex, toIndex);
    }
}
