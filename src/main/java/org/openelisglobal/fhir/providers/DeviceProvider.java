package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.DeviceSearchParams;
import org.openelisglobal.search.service.DeviceSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Read and search Device resources from OpenELIS. */
@Component
public class DeviceProvider implements IResourceProvider {

    @Autowired
    private DeviceSearchService deviceSearchService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Device.class;
    }

    @Read
    public Device readDevice(@IdParam IdType theId) {

        String method = "readDevice";

        try {
            Analyzer analyzer = requireAnalyzer(theId, method);

            Device device = fhirTransformService.transformAnalyzerToDevice(analyzer);

            if (device == null) {
                throw new InternalErrorException("Unable to transform Analyzer to Device");
            }

            return device;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Device", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method, e.getMessage());
            throw new InternalErrorException("Unexpected error reading Device", e);
        }
    }

    @Search
    public IBundleProvider searchDevices(@OptionalParam(name = Device.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = Device.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Device.SP_DEVICE_NAME) StringAndListParam deviceName,
            @OptionalParam(name = Device.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Device.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count, HttpServletRequest request) {

        String method = "searchDevices";
        LogEvent.logDebug(getClass().getSimpleName(), method, "Searching for Devices");

        try {
            DeviceSearchParams params = new DeviceSearchParams(id, identifier, deviceName, type, status, lastUpdated,
                    sort);
            return FhirProviderUtils.withPaging(deviceSearchService.searchDevices(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(getClass().getSimpleName(), method, "Invalid Device search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Device search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Device", e);
            }
            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());
            throw new InternalErrorException("Error searching Device", e);
        }
    }

    /**
     * Resolves the Analyzer addressed by a resource id: 400 for a missing or
     * non-UUID id, 404 when nothing matches.
     */
    private Analyzer requireAnalyzer(IdType theId, String method) {
        FhirProviderUtils.validateIdParam(theId, "Device", getClass().getSimpleName(), method);

        UUID uuid;
        try {
            uuid = UUID.fromString(theId.getIdPart());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Device ID must be a valid UUID");
        }

        List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", uuid);

        if (analyzers == null || analyzers.isEmpty()) {
            throw new ResourceNotFoundException("Device/" + theId.getIdPart());
        }

        if (analyzers.size() > 1) {
            LogEvent.logError(getClass().getSimpleName(), method, "Multiple analyzers found for " + theId.getIdPart());
            throw new InternalErrorException("Multiple Analyzer records exist for Device UUID");
        }

        return analyzerService.getWithMapping(analyzers.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Device/" + theId.getIdPart()));
    }

}
