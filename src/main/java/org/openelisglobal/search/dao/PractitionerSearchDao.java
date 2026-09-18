package org.openelisglobal.search.dao;

import java.util.List;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO for searching OpenELIS Provider records using FHIR Practitioner search
 * parameters.
 */
@Repository
@Transactional(readOnly = true)
public class PractitionerSearchDao extends BaseFhirDao {

    private final FacadeHelperDao facadeHelperDao;

    public PractitionerSearchDao(FhirPropertyResolver propertyResolver, FacadeHelperDao facadeHelperDao) {

        super(propertyResolver);
        this.facadeHelperDao = facadeHelperDao;
    }

    /**
     * Searches for all providers matching the supplied Practitioner search
     * parameters.
     *
     * @param params Practitioner search parameters
     * @return matching providers
     */
    public List<Provider> search(PractitionerSearchParams params) {

        FhirCriteriaContext<Provider, Provider> context = createCriteriaContext(Provider.class);

        if (params != null) {
            addSearchPredicates(context, params);
        }

        context.distinct(true);

        return list(context);
    }

    /**
     * Searches for providers using zero-based pagination.
     *
     * @param params   Practitioner search parameters
     * @param offset   zero-based result offset
     * @param pageSize maximum number of records to return
     * @return matching providers
     */
    public List<Provider> search(PractitionerSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        FhirCriteriaContext<Provider, Provider> context = createCriteriaContext(Provider.class);

        if (params != null) {
            addSearchPredicates(context, params);
        }

        context.distinct(true);

        return list(context, offset, pageSize);
    }

    /**
     * Counts providers matching the supplied Practitioner search parameters.
     *
     * @param params Practitioner search parameters
     * @return number of matching providers
     */
    public long count(PractitionerSearchParams params) {

        return count(Provider.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /**
     * Adds all supported Practitioner search predicates.
     *
     * <p>
     * The generic result type permits this method to be reused by both entity
     * searches and count queries.
     * </p>
     */
    private <R> void addSearchPredicates(FhirCriteriaContext<Provider, R> context, PractitionerSearchParams params) {

        /*
         * FHIR logical resource ID:
         *
         * Practitioner?_id=<uuid>
         */
        addPredicate(context, createIdPredicate(context, params.getId()));

        /*
         * FHIR Practitioner business identifier.
         *
         * Uses the original BaseFhirDao identifier handling, which maps the identifier
         * value to the Provider FHIR UUID field.
         *
         * NPI and external ID are intentionally not included here.
         */
        addPredicate(context, createIdentifierPredicate(context, params.getIdentifier()));

        /*
         * Standard HumanName search across given and family name.
         */
        addPredicate(context, facadeHelperDao.createNamePredicate(context, params.getName()));

        addPredicate(context,
                createStringPredicate(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER, params.getGiven()));

        addPredicate(context,
                createStringPredicate(context, FhirConstants.LAST_NAME_SEARCH_HANDLER, params.getFamily()));
        addPredicate(context, createStringPredicate(context, FhirConstants.CITY_SEARCH_HANDLER, params.getCity()));

        addPredicate(context, createStringPredicate(context, FhirConstants.STATE_SEARCH_HANDLER, params.getState()));

        addPredicate(context,
                createStringPredicate(context, FhirConstants.POSTALCODE_SEARCH_HANDLER, params.getPostalCode()));

        addPredicate(context,
                createStringPredicate(context, FhirConstants.COUNTRY_SEARCH_HANDLER, params.getCountry()));

        /*
         * Searches every ContactPoint-related field.
         */
        addPredicate(context, facadeHelperDao.createTelecomPredicate(context, params.getTelecom()));

        /*
         * Searches only the email field.
         */
        addPredicate(context, facadeHelperDao.createEmailPredicate(context, params.getEmail()));

        /*
         * Searches telephone-related fields, excluding email and fax.
         */
        addPredicate(context, facadeHelperDao.createPhonePredicate(context, params.getPhone()));

        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /** Providers by primary key, for the ServiceRequest requester include. */
    public List<Provider> findByIds(List<String> providerIds) {

        List<String> ids = providerIds == null ? List.of()
                : providerIds.stream().filter(java.util.Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty())
                        .distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        FhirCriteriaContext<Provider, Provider> context = createCriteriaContext(Provider.class);

        context.addPredicate(context.getRoot().get("id").in(ids));

        context.distinct(true);

        return list(context);
    }
}