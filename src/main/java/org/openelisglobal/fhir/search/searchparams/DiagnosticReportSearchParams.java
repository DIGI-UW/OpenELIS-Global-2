package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.DiagnosticReport;

/**
 * Search parameters the DiagnosticReport facade answers from the OpenELIS
 * database: logical id, patient, order ({@code based-on}), result, specimen,
 * test code, status, issued date and last-updated timestamp.
 *
 * <p>
 * {@code patient} and {@code subject} arrive merged. The OpenELIS analysis
 * status ids behind each FHIR status code are supplied by the search service.
 */
public class DiagnosticReportSearchParams extends BaseSearchParam {

    private ReferenceAndListParam patient;

    private ReferenceAndListParam basedOn;

    private ReferenceAndListParam result;

    private ReferenceAndListParam specimen;

    private TokenAndListParam code;

    private TokenAndListParam status;

    private DateRangeParam issued;

    private Map<String, List<String>> statusIdsByCode = Collections.emptyMap();

    private final Set<Include> includes;

    public DiagnosticReportSearchParams(TokenAndListParam id, TokenAndListParam identifier,
            ReferenceAndListParam patient, ReferenceAndListParam basedOn, ReferenceAndListParam result,
            ReferenceAndListParam specimen, TokenAndListParam code, TokenAndListParam status, DateRangeParam issued,
            DateRangeParam lastUpdated, SortSpec sort, Set<Include> includes) {

        super(id, identifier, lastUpdated, sort);

        this.patient = patient;
        this.basedOn = basedOn;
        this.result = result;
        this.specimen = specimen;
        this.code = code;
        this.status = status;
        this.issued = issued;
        this.includes = includes == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(includes));
    }

    @Override
    public SearchParameterMap toSearchParameterMap() {

        SearchParameterMap map = new SearchParameterMap();
        addBaseSearchParameters(map);

        if (patient != null) {
            map.addParameter(DiagnosticReport.SP_PATIENT, patient);
        }
        if (basedOn != null) {
            map.addParameter(DiagnosticReport.SP_BASED_ON, basedOn);
        }
        if (result != null) {
            map.addParameter(DiagnosticReport.SP_RESULT, result);
        }
        if (specimen != null) {
            map.addParameter(DiagnosticReport.SP_SPECIMEN, specimen);
        }
        if (code != null) {
            map.addParameter(DiagnosticReport.SP_CODE, code);
        }
        if (status != null) {
            map.addParameter(DiagnosticReport.SP_STATUS, status);
        }
        if (issued != null) {
            map.addParameter(DiagnosticReport.SP_ISSUED, issued);
        }
        if (getSort() != null) {
            map.setSortSpec(getSort());
        }
        return map;
    }

    public ReferenceAndListParam getPatient() {
        return patient;
    }

    public ReferenceAndListParam getBasedOn() {
        return basedOn;
    }

    public ReferenceAndListParam getResult() {
        return result;
    }

    public ReferenceAndListParam getSpecimen() {
        return specimen;
    }

    public TokenAndListParam getCode() {
        return code;
    }

    public TokenAndListParam getStatus() {
        return status;
    }

    public DateRangeParam getIssued() {
        return issued;
    }

    public Map<String, List<String>> getStatusIdsByCode() {
        return statusIdsByCode;
    }

    public void setStatusIdsByCode(Map<String, List<String>> statusIdsByCode) {
        this.statusIdsByCode = statusIdsByCode == null ? Collections.emptyMap() : statusIdsByCode;
    }

    public boolean hasInclude(String... includeValues) {
        for (Include include : includes) {
            for (String value : includeValues) {
                if (value != null && value.equals(include.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }
}
