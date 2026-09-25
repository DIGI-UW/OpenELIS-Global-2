package org.openelisglobal.fhir.search.searchparams;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import java.io.Serializable;
import org.openelisglobal.fhir.FhirConstants;

/**
 * Base class for common FHIR search parameters.
 *
 * <p>
 * Search parameter values remain as HAPI FHIR parameter objects so that
 * Criteria-based search DAOs can correctly preserve:
 * </p>
 *
 * <ul>
 * <li>Repeated parameter AND semantics</li>
 * <li>Comma-separated parameter OR semantics</li>
 * <li>Date prefixes and ranges</li>
 * <li>Token systems and values</li>
 * <li>FHIR sort specifications</li>
 * </ul>
 */
public abstract class BaseSearchParam implements Serializable {

    private static final long serialVersionUID = 1L;

    private TokenAndListParam id;

    private TokenAndListParam identifier;

    private DateRangeParam lastUpdated;

    private SortSpec sort;

    protected BaseSearchParam(TokenAndListParam id, TokenAndListParam identifier, DateRangeParam lastUpdated) {

        this(id, identifier, lastUpdated, null);
    }

    protected BaseSearchParam(TokenAndListParam id, TokenAndListParam identifier, DateRangeParam lastUpdated,
            SortSpec sort) {

        this.id = id;
        this.identifier = identifier;
        this.lastUpdated = lastUpdated;
        this.sort = sort;
    }

    protected void addBaseSearchParameters(SearchParameterMap map) {

        if (getId() != null) {
            map.addParameter(FhirConstants.ID_PROPERTY, getId());
        }

        if (getIdentifier() != null) {
            map.addParameter(FhirConstants.IDENTIFIER_SEARCH_HANDLER, getIdentifier());
        }

        if (getLastUpdated() != null) {
            map.addParameter(FhirConstants.LAST_UPDATED_PROPERTY, getLastUpdated());
        }
    }

    /**
     * Returns this resource's search parameter map.
     *
     * <p>
     * This can remain available for metadata, validation, logging, or compatibility
     * with existing code. The Criteria search DAO should read the typed properties
     * directly from this object.
     * </p>
     */
    public abstract SearchParameterMap toSearchParameterMap();

    public TokenAndListParam getId() {
        return id;
    }

    public void setId(TokenAndListParam id) {

        this.id = id;
    }

    public TokenAndListParam getIdentifier() {
        return identifier;
    }

    public void setIdentifier(TokenAndListParam identifier) {

        this.identifier = identifier;
    }

    public DateRangeParam getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(DateRangeParam lastUpdated) {

        this.lastUpdated = lastUpdated;
    }

    public SortSpec getSort() {
        return sort;
    }

    public void setSort(SortSpec sort) {

        this.sort = sort;
    }
}
