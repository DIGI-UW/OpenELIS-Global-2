package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR provider for DiagnosticReport resources backed by OpenELIS Analysis
 * data.
 */
@Component
public class DiagnosticReportProvider implements IResourceProvider {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirUtil util;
    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return DiagnosticReport.class;
    }

    @Read
    public DiagnosticReport readDiagnosticReport(@IdParam IdType theId) {
        String method = "readDiagnosticReport";
        try {
            FhirProviderUtils.validateIdParam(theId, "DiagnosticReport", this.getClass().getSimpleName(), method);

            List<Analysis> matches = analysisService.getAllMatching("fhirUuid", UUID.fromString(theId.getIdPart()));
            if (matches == null || matches.isEmpty()) {
                throw new ResourceNotFoundException("DiagnosticReport/" + theId.getIdPart());
            }
            if (matches.size() > 1) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "Duplicate Analysis records found for fhirUuid=" + theId.getIdPart());
                throw new InternalErrorException("Multiple Analysis records found for DiagnosticReport UUID");
            }

            Analysis analysis = matches.get(0);
            DiagnosticReport report = fhirTransformService.transformResultToDiagnosticReport(analysis);
            if (report == null) {
                throw new InternalErrorException("Failed to transform Analysis to DiagnosticReport");
            }

            return report;

        } catch (InvalidRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("DiagnosticReport ID must be a valid UUID");
        } catch (FhirTransformationException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "FHIR transformation error while reading DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("FHIR transformation failed for DiagnosticReport", e);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while reading DiagnosticReport", e);
        }
    }

    @Create
    public MethodOutcome createDiagnosticReport(@ResourceParam DiagnosticReport report, HttpServletRequest request) {
        final String method = "createDiagnosticReport";

        if (report == null) {
            throw new InvalidRequestException("DiagnosticReport payload is required");
        }

        String sysUserId = FhirProviderUtils.getSysUserId(request);
        if (sysUserId == null) {
            throw new AuthenticationException("Unable to resolve system user");
        }

        try {
            ResultsUpdateDataSet dataset = fhirTransformService.createResultUpdateDataSetFromReport(report, sysUserId);

            if (dataset == null) {
                throw new UnprocessableEntityException("Failed to transform DiagnosticReport into dataset");
            }

            List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

            logbookResultsPersistService.persistDataSet(dataset, updaters, sysUserId);

            if (dataset.getModifiedAnalysis() == null || dataset.getModifiedAnalysis().isEmpty()) {
                throw new UnprocessableEntityException("No Analysis created from DiagnosticReport");
            }

            Analysis analysis = dataset.getModifiedAnalysis().get(0);
            if (analysis == null) {
                throw new UnprocessableEntityException("Analysis transformation failed");
            }

            DiagnosticReport createdReport = fhirTransformService.transformResultToDiagnosticReport(analysis);

            if (createdReport == null) {
                throw new InternalErrorException("Failed to transform Analysis to DiagnosticReport");
            }
            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, createdReport, this.getClass().getSimpleName(),
                    method);

            return FhirProviderUtils.buildUpdateOutcome(createdReport);

        } catch (FhirTransformationException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "FHIR transformation error: " + e.getMessage());
            throw new UnprocessableEntityException("Invalid DiagnosticReport structure", e);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while creating DiagnosticReport", e);
        }
    }

    @Update
    public MethodOutcome updateDiagnosticReport(@ResourceParam DiagnosticReport report, @IdParam IdType theId,
            HttpServletRequest request) {
        final String method = "updateDiagnosticReport";
        if (theId == null || theId.getIdPart() == null) {
            throw new InvalidRequestException("Missing DiagnosticReport ID in URL");
        }

        if (report == null) {
            throw new InvalidRequestException("DiagnosticReport payload is required");
        }

        String sysUserId = FhirProviderUtils.getSysUserId(request);
        if (sysUserId == null) {
            throw new AuthenticationException("Unable to resolve system user");
        }

        try {
            ResultsUpdateDataSet dataset = fhirTransformService.createResultUpdateDataSetFromReport(report, sysUserId);

            if (dataset == null) {
                throw new UnprocessableEntityException("Failed to transform DiagnosticReport into dataset");
            }

            List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();

            logbookResultsPersistService.persistDataSet(dataset, updaters, sysUserId);

            if (dataset.getModifiedAnalysis() == null || dataset.getModifiedAnalysis().isEmpty()) {
                throw new UnprocessableEntityException("No Analysis created from DiagnosticReport");
            }

            Analysis analysis = dataset.getModifiedAnalysis().get(0);
            if (analysis == null) {
                throw new UnprocessableEntityException("Analysis transformation failed");
            }

            DiagnosticReport createdReport = fhirTransformService.transformResultToDiagnosticReport(analysis);

            if (createdReport == null) {
                throw new InternalErrorException("Failed to transform Analysis to DiagnosticReport");
            }
            FhirProviderUtils.syncToFhirStore(fhirPersistanceService, createdReport, this.getClass().getSimpleName(),
                    method);

            return FhirProviderUtils.buildUpdateOutcome(createdReport);

        } catch (FhirTransformationException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "FHIR transformation error: " + e.getMessage());
            throw new UnprocessableEntityException("Invalid DiagnosticReport structure", e);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while creating DiagnosticReport", e);
        }
    }

    @Delete
    public MethodOutcome deleteDiagnosticReport(@IdParam IdType theId, HttpServletRequest request) {
        final String method = "deleteDiagnosticReport";

        try {
            if (theId == null || theId.getIdPart() == null) {
                throw new InvalidRequestException("Missing DiagnosticReport ID in URL");
            }

            String sysUserId = FhirProviderUtils.getSysUserId(request);
            String analysisUuid = theId.getIdPart();

            List<Analysis> existingAnalyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));
            if (existingAnalyses.isEmpty()) {
                throw new ResourceNotFoundException("Analysis not found with UUID: " + analysisUuid);
            }

            Analysis analysis = existingAnalyses.get(0);

            analysis = analysisService.get(analysis.getId());

            // Cancel the analysis
            analysis.setSysUserId(sysUserId);
            analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled));

            Analysis updatedAnalysis = analysisService.update(analysis);

            try {
                fhirTransformService.transformAnalysisByIds(List.of(updatedAnalysis.getId()));
            } catch (Exception fhirEx) {
                LogEvent.logWarn(this.getClass().getSimpleName(), method,
                        "FHIR sync failed during delete (non-blocking): " + safeMessage(fhirEx));
            }

            MethodOutcome outcome = new MethodOutcome();
            outcome.setResponseStatusCode(204);
            return outcome;

        } catch (InvalidRequestException | ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));
            throw e;
        } catch (InternalErrorException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Internal error: " + safeMessage(e));
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));
            throw new InternalErrorException(
                    "Unexpected server error while deleting DiagnosticReport: " + e.getMessage(), e);
        }
    }

    @Search
    public Bundle searchDiagnosticReports(
            @OptionalParam(name = DiagnosticReport.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
                    Patient.SP_GIVEN, Patient.SP_FAMILY,
                    Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam subject,
            @OptionalParam(name = DiagnosticReport.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = DiagnosticReport.SP_DATE) DateRangeParam date, HttpServletRequest request) {
        String method = "searchDiagnosticReports";
        try {
            Bundle resultBundle = util.forwardSearchToFhirStore(request);

            if (resultBundle == null) {
                resultBundle = new Bundle();
            }

            if (resultBundle.getType() == null) {
                resultBundle.setType(Bundle.BundleType.SEARCHSET);
            }

            if (resultBundle.getEntry() == null) {
                resultBundle.setEntry(new ArrayList<>());
            }

            return resultBundle;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching DiagnosticReports: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching DiagnosticReports", e);
        }
    }

    private String safeMessage(Exception e) {
        return (e == null || e.getMessage() == null) ? "No error message available" : e.getMessage();
    }

}
