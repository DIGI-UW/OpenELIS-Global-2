package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.DiagnosticReportSearchParams;
import org.openelisglobal.search.service.DiagnosticReportSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR provider for DiagnosticReport resources backed by OpenELIS Analysis
 * data.
 *
 * <p>
 * A DiagnosticReport is a projection of an Analysis and its Results, so it has
 * no create or update path of its own: orders arrive as ServiceRequest and
 * results as Observation. DELETE cancels the underlying Analysis rather than
 * removing rows.
 *
 * <p>
 * Supported operations:
 * <ul>
 * <li>READ: GET /fhir/DiagnosticReport/{uuid}</li>
 * <li>SEARCH: GET /fhir/DiagnosticReport?patient={uuid}&amp;... (answered from
 * the OpenELIS database)</li>
 * <li>DELETE: DELETE /fhir/DiagnosticReport/{uuid} (cancels the Analysis)</li>
 * </ul>
 */
@Component
public class DiagnosticReportProvider implements IResourceProvider {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private DiagnosticReportSearchService diagnosticReportSearchService;

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
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("DiagnosticReport", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while reading DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while reading DiagnosticReport", e);
        }
    }

    /**
     * Cancels the Analysis behind the report. The rows stay in place so the audit
     * trail survives; a subsequent read returns the report with the cancelled
     * status.
     */
    @Delete
    public MethodOutcome deleteDiagnosticReport(@IdParam IdType theId, HttpServletRequest request) {
        final String method = "deleteDiagnosticReport";

        try {
            FhirProviderUtils.validateIdParam(theId, "DiagnosticReport", this.getClass().getSimpleName(), method);

            String analysisUuid = theId.getIdPart();
            List<Analysis> existingAnalyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));
            if (existingAnalyses.isEmpty()) {
                throw new ResourceNotFoundException("DiagnosticReport/" + analysisUuid);
            }

            Analysis analysis = analysisService.get(existingAnalyses.get(0).getId());
            analysis.setSysUserId(FhirProviderUtils.getSysUserId(request));
            analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Canceled));

            Analysis updatedAnalysis = analysisService.update(analysis);

            try {
                fhirTransformService.transformAnalysisByIds(List.of(updatedAnalysis.getId()));
            } catch (Exception fhirEx) {
                LogEvent.logWarn(this.getClass().getSimpleName(), method,
                        "FHIR sync failed during delete (non-blocking): " + FhirProviderUtils.safeMessage(fhirEx));
            }

            return FhirProviderUtils.buildDeleteOutcome(theId, "DiagnosticReport");

        } catch (InvalidRequestException | ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Client error: " + FhirProviderUtils.safeMessage(e));
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("DiagnosticReport ID must be a valid UUID");
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("DiagnosticReport", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unhandled exception: " + FhirProviderUtils.safeMessage(e));
            throw new InternalErrorException(
                    "Unexpected server error while deleting DiagnosticReport: " + e.getMessage(), e);
        }
    }

    @Search
    public IBundleProvider searchDiagnosticReports(
            @OptionalParam(name = DiagnosticReport.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = DiagnosticReport.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = DiagnosticReport.SP_PATIENT) ReferenceAndListParam patient,
            @OptionalParam(name = DiagnosticReport.SP_SUBJECT) ReferenceAndListParam subject,
            @OptionalParam(name = DiagnosticReport.SP_BASED_ON) ReferenceAndListParam basedOn,
            @OptionalParam(name = DiagnosticReport.SP_RESULT) ReferenceAndListParam result,
            @OptionalParam(name = DiagnosticReport.SP_SPECIMEN) ReferenceAndListParam specimen,
            @OptionalParam(name = DiagnosticReport.SP_CODE) TokenAndListParam code,
            @OptionalParam(name = DiagnosticReport.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = DiagnosticReport.SP_ISSUED) DateRangeParam issued,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.DIAGNOSTIC_REPORT_PATIENT_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_SUBJECT_INCLUDE, FhirConstants.DIAGNOSTIC_REPORT_BASED_ON_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_RESULT_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_SPECIMEN_INCLUDE }) HashSet<Include> includes,
            HttpServletRequest request) {

        String method = "searchDiagnosticReports";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Searching for DiagnosticReports");

        try {
            DiagnosticReportSearchParams params = new DiagnosticReportSearchParams(id, identifier,
                    FhirProviderUtils.merge(patient, subject), basedOn, result, specimen, code, status, issued,
                    lastUpdated, sort, includes);
            return FhirProviderUtils.withPaging(diagnosticReportSearchService.searchDiagnosticReports(params), offset,
                    count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Invalid DiagnosticReport search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid DiagnosticReport search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("DiagnosticReport", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching DiagnosticReports: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching DiagnosticReports", e);
        }
    }

}
