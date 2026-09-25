package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.searchparams.DeviceSearchParams;
import org.openelisglobal.search.service.DeviceSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 provider exposing OpenELIS Analyzers as Device resources.
 *
 * <p>
 * Read and write operations hit the OpenELIS database directly; the result is
 * mirrored to the FHIR store on a best-effort basis; search is answered from
 * the analyzer table.
 *
 * <p>
 * Supported operations:
 * <ul>
 * <li>READ: GET /fhir/Device/{uuid}</li>
 * <li>CREATE: POST /fhir/Device (a client-supplied id is ignored)</li>
 * <li>UPDATE: PUT /fhir/Device/{uuid} (the URL id is authoritative)</li>
 * <li>DELETE: DELETE /fhir/Device/{uuid} (deactivates the Analyzer)</li>
 * <li>SEARCH: GET /fhir/Device?identifier=...</li>
 * </ul>
 */
@Component
public class DeviceProvider implements IResourceProvider {

    @Autowired
    private DeviceSearchService deviceSearchService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

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

    @Create
    public MethodOutcome createDevice(@ResourceParam Device device, HttpServletRequest request) {

        String method = "createDevice";

        try {

            if (device == null) {
                throw new InvalidRequestException("Device resource is required");
            }

            // FHIR create: the server assigns the id, so a client-supplied one must not
            // silently turn this into an update of an existing Analyzer.
            device.setId((String) null);

            Analyzer analyzer = fhirTransformService.transformDeviceToAnalyzer(device);

            if (analyzer == null) {
                throw new UnprocessableEntityException("Unable to transform Device into Analyzer");
            }

            String userId = FhirProviderUtils.getSysUserId(request);

            analyzer.setSysUserId(userId);

            if (analyzer.getAnalyzerType() != null) {
                analyzer.getAnalyzerType().setSysUserId(userId);
            }

            Analyzer saved = analyzerService.save(analyzer);

            if (saved == null) {
                throw new InternalErrorException("Analyzer was not saved");
            }

            Device savedDevice = fhirTransformService.transformAnalyzerToDevice(saved);

            if (savedDevice == null) {
                throw new InternalErrorException("Unable to create FHIR Device");
            }

            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, savedDevice, getClass().getSimpleName(), method);

            return FhirProviderUtils.buildCreateOutcome(savedDevice);

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Device", e);
            }
            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());
            throw new InternalErrorException("Unexpected error creating Device", e);
        }

    }

    @Update
    public MethodOutcome updateDevice(@IdParam IdType theId, @ResourceParam Device device, HttpServletRequest request) {

        String method = "updateDevice";

        try {

            if (device == null) {
                throw new InvalidRequestException("Device resource required");
            }

            requireAnalyzer(theId, method);

            if (device.hasId() && !theId.getIdPart().equals(device.getIdElement().getIdPart())) {
                throw new InvalidRequestException("Device.id " + device.getIdElement().getIdPart()
                        + " does not match the id in the request URL " + theId.getIdPart());
            }

            device.setId(theId.getIdPart());

            Analyzer analyzer = fhirTransformService.transformDeviceToAnalyzer(device);

            if (analyzer == null) {
                throw new UnprocessableEntityException("Cannot transform Device");
            }

            analyzer.setSysUserId(FhirProviderUtils.getSysUserId(request));

            Analyzer updated = analyzerService.update(analyzer);

            if (updated == null) {
                throw new InternalErrorException("Analyzer update failed");
            }

            Device updatedDevice = fhirTransformService.transformAnalyzerToDevice(updated);

            if (updatedDevice == null) {
                throw new InternalErrorException("FHIR Device transformation failed");
            }

            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, updatedDevice, getClass().getSimpleName(),
                    method);

            return FhirProviderUtils.buildUpdateOutcome(updatedDevice);

        } catch (UnprocessableEntityException | InvalidRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Device", e);
            }
            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());
            throw new InternalErrorException("Unexpected error updating Device", e);
        }
    }

    /**
     * Deactivates the Analyzer instead of removing it, and marks it inactive so the
     * mirrored Device no longer reports an active status.
     */
    @Delete
    public MethodOutcome deleteDevice(@IdParam IdType theId, HttpServletRequest request) {

        String method = "deleteDevice";

        try {
            Analyzer analyzer = requireAnalyzer(theId, method);

            analyzer.setActive(false);
            analyzer.setStatus(Analyzer.AnalyzerStatus.INACTIVE);
            analyzer.setSysUserId(FhirProviderUtils.getSysUserId(request));

            Analyzer saved = analyzerService.save(analyzer);

            if (saved == null) {
                throw new InternalErrorException("Failed deleting Device");
            }

            Device deleted = fhirTransformService.transformAnalyzerToDevice(saved);

            if (deleted != null) {
                FhirProviderUtils.syncToFhirStore(fhirPersistanceService, deleted, getClass().getSimpleName(), method);
            }

            return FhirProviderUtils.buildDeleteOutcome(theId, "Device");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Device", e);
            }
            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());
            throw new InternalErrorException("Unexpected error deleting Device", e);
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

        return analyzers.get(0);
    }

}
