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
import org.hl7.fhir.r4.model.Practitioner;
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
                throw new IllegalArgumentException("Device ID must be provided");
            }
            String analyzerId = theId.getIdPart();
            List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", UUID.fromString(analyzerId));
            if (analyzers == null || analyzers.isEmpty()) {
                throw new ResourceNotFoundException("Analyzer with FHIR ID: " + analyzerId + " does not exist");
            }
            if (analyzers.size() > 1) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "Duplicate Analyzer records found for fhirUuid=" + analyzerId);
                throw new InternalErrorException("Multiple Analyzer records found for ServiceRequest UUID");
            }
            Analyzer analyzer = analyzers.get(0);
            Device device = fhirTransformService.transformAnalyzerToDevice(analyzer);
            if (device == null) {
                throw new ResourceNotFoundException(
                        "No Device resource could be created for Analyzer with FHIR ID: " + analyzerId);
            }
            return device;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Device ID must be a valid UUID");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Device: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Device", e);
        }

    }

    @Create
    public MethodOutcome createDevice(@ResourceParam Device device, HttpServletRequest request) {
        String method = "createDevice";
        try {
            if (device == null) {
                throw new IllegalArgumentException("Device resource must be provided");
            }
            Analyzer analyzer = fhirTransformService.transformDeviceToAnalyzer(device);
            if (analyzer == null) {
                throw new UnprocessableEntityException(
                        "Provided Device resource could not be transformed to an Analyzer");
            }
            analyzer.getAnalyzerType().setSysUserId(FhirProviderUtils.getSysUserId(request));
            analyzer.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Analyzer createdAnalyzer = analyzerService.save(analyzer);
            if (createdAnalyzer == null) {
                throw new InternalErrorException(
                        "Failed to persist the Analyzer created from the provided Device resource");
            }
            Device createdDevice = fhirTransformService.transformAnalyzerToDevice(createdAnalyzer);
            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, createdDevice, this.getClass().getSimpleName(),
                    method);
            return FhirProviderUtils.buildCreateOutcome(createdDevice);
        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Practitioner", e);
        }

    }

    @Update
    public MethodOutcome updateDevice(@IdParam IdType theId, @ResourceParam Device device, HttpServletRequest request) {
        String method = "createDevice";
        try {
            if (theId == null || theId.isEmpty()) {
                throw new IllegalArgumentException("Device ID must be provided");
            }
            if (device == null) {
                throw new IllegalArgumentException("Device resource must be provided");
            }
            Analyzer analyzer = fhirTransformService.transformDeviceToAnalyzer(device);
            if (analyzer == null) {
                throw new UnprocessableEntityException(
                        "Provided Device resource could not be transformed to an Analyzer");
            }
            analyzer.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Analyzer createdAnalyzer = analyzerService.update(analyzer);
            if (createdAnalyzer == null) {
                throw new InternalErrorException(
                        "Failed to persist the Analyzer created from the provided Device resource");
            }
            Device createdDevice = fhirTransformService.transformAnalyzerToDevice(createdAnalyzer);
            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, createdDevice, this.getClass().getSimpleName(),
                    method);
            return FhirProviderUtils.buildUpdateOutcome(createdDevice);
        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Practitioner", e);
        }

    }

    @Delete
    public MethodOutcome deleteDevice(@IdParam IdType theId, HttpServletRequest request) {
        String method = "deleteDevice";
        try {
            if (theId == null || theId.isEmpty()) {
                throw new IllegalArgumentException("Device ID must be provided");
            }
            String analyzerId = theId.getIdPart();
            List<Analyzer> analyzers = analyzerService.getAllMatching("fhirUuid", UUID.fromString(analyzerId));
            if (analyzers == null || analyzers.isEmpty()) {
                throw new ResourceNotFoundException("Device with FHIR ID: " + analyzerId + " does not exist");
            }
            if (analyzers.size() > 1) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "Duplicate Analyzer records found for fhirUuid=" + analyzerId);
                throw new InternalErrorException("Multiple Analysis records found for ServiceRequest UUID");
            }
            Analyzer analyzer = analyzers.getFirst();
            analyzer.setActive(false);
            analyzer.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Analyzer createdAnalyzer = analyzerService.save(analyzer);
            if (createdAnalyzer == null) {
                throw new InternalErrorException(
                        "Failed to persist the Analyzer created from the provided Device resource");
            }
            Device deletedDevice = fhirTransformService.transformAnalyzerToDevice(createdAnalyzer);
            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, deletedDevice, this.getClass().getSimpleName(),
                    method);
            return FhirProviderUtils.buildDeleteOutcome(theId, "Device");
        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Practitioner", e);
        }

    }

    @Search
    public Bundle searchPractitionerBundle(
            @OptionalParam(name = Practitioner.SP_IDENTIFIER) TokenAndListParam identifier,
            @IncludeParam(reverse = true, allow = { "Encounter:" + Encounter.SP_PARTICIPANT,
                    "ServiceRequest:" + ServiceRequest.SP_REQUESTER, }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchDeviceBundle";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName,
                "Searching for Practitioners (returning Bundle)");

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Devices: " + e.getMessage());
            throw new InternalErrorException("Error searching Practitioners", e);
        }
    }

}
