package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeviceProvider implements IResourceProvider {

    @Autowired
    private FhirUtil util;

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

            if (theId == null || theId.isEmpty()) {
                throw new InvalidRequestException("Device ID must be provided");
            }

            String analyzerId = theId.getIdPart();

            UUID uuid;

            try {
                uuid = UUID.fromString(analyzerId);
            } catch (Exception e) {
                throw new InvalidRequestException("Device ID must be a valid UUID");
            }

            List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", uuid);

            if (analyzers == null || analyzers.isEmpty()) {
                throw new ResourceNotFoundException("Device with ID " + analyzerId + " not found");
            }

            if (analyzers.size() > 1) {

                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "Multiple analyzers found for " + analyzerId);

                throw new InternalErrorException("Multiple Analyzer records exist for Device UUID");
            }

            Device device = fhirTransformService.transformAnalyzerToDevice(analyzers.get(0));

            if (device == null) {
                throw new InternalErrorException("Unable to transform Analyzer to Device");
            }

            return device;

        } catch (ResourceNotFoundException | InvalidRequestException e) {

            throw e;

        } catch (Exception e) {

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

            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());

            throw new InternalErrorException("Unexpected error creating Device", e);
        }

    }

    @Update
    public MethodOutcome updateDevice(@IdParam IdType theId, @ResourceParam Device device, HttpServletRequest request) {

        String method = "updateDevice";

        try {

            if (theId == null || theId.isEmpty()) {

                throw new InvalidRequestException("Device ID required");
            }

            if (device == null) {

                throw new InvalidRequestException("Device resource required");
            }

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

        } catch (UnprocessableEntityException | InvalidRequestException e) {

            throw e;
        } catch (Exception e) {

            throw new InternalErrorException("Unexpected error updating Device", e);
        }
    }

    @Delete
    public MethodOutcome deleteDevice(@IdParam IdType theId, HttpServletRequest request) {

        String method = "deleteDevice";

        try {

            if (theId == null || theId.isEmpty()) {

                throw new InvalidRequestException("Device ID required");
            }

            String id = theId.getIdPart();

            UUID uuid;

            try {
                uuid = UUID.fromString(id);
            } catch (Exception e) {

                throw new InvalidRequestException("Device ID must be UUID");
            }

            List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", uuid);

            if (analyzers == null || analyzers.isEmpty()) {

                throw new ResourceNotFoundException("Device not found");
            }

            if (analyzers.size() > 1) {

                throw new InternalErrorException("Multiple Analyzer records found");
            }

            Analyzer analyzer = analyzers.get(0);

            analyzer.setActive(false);

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

            throw new InternalErrorException("Unexpected error deleting Device", e);
        }
    }

    @Search
    public Bundle searchDeviceBundle(@OptionalParam(name = Device.SP_IDENTIFIER) TokenAndListParam identifier,

            @OptionalParam(name = Device.SP_DEVICE_NAME) TokenAndListParam deviceName,

            @OptionalParam(name = Device.SP_TYPE) TokenAndListParam type,

            @IncludeParam(reverse = true, allow = { "Encounter:" + Encounter.SP_PARTICIPANT,
                    "ServiceRequest:" + ServiceRequest.SP_REQUESTER }) HashSet<Include> revIncludes,

            HttpServletRequest request) {

        String method = "searchDeviceBundle";

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            if (bundle == null) {

                throw new InternalErrorException("FHIR server returned empty Bundle");
            }

            return bundle;

        } catch (Exception e) {

            LogEvent.logError(getClass().getSimpleName(), method, e.getMessage());

            throw new InternalErrorException("Error searching Device", e);
        }
    }

}