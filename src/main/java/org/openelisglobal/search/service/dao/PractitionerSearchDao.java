package org.openelisglobal.search.service.dao;

import ca.uhn.fhir.rest.param.StringAndListParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    private final FhirPropertyResolver propertyResolver;

    public PractitionerSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
        this.propertyResolver = Objects.requireNonNull(propertyResolver, "FhirPropertyResolver must not be null");
    }

    /**
     * Searches for providers matching the supplied FHIR Practitioner search
     * parameters.
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
     * Searches for providers with pagination.
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
     * This uses the generic count helper provided by BaseFhirDao and reuses the
     * same predicates used by the normal and paginated search methods.
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
     * The result type is generic so this method can be reused by both the Provider
     * result query and the Long count query.
     */
    private <R> void addSearchPredicates(FhirCriteriaContext<Provider, R> context, PractitionerSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createNamePredicate(context, params.getName()));

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

        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /**
     * FHIR Practitioner.name searches across given and family names.
     */
    private <R> Optional<Predicate> createNamePredicate(FhirCriteriaContext<Provider, R> context,
            StringAndListParam name) {

        if (name == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();

        Expression<String> givenExpression = propertyResolver.resolve(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER,
                String.class);

        Expression<String> familyExpression = propertyResolver.resolve(context, FhirConstants.LAST_NAME_SEARCH_HANDLER,
                String.class);

        return handleStringAndListParam(criteriaBuilder, name, parameter -> {
            Optional<Predicate> givenPredicate = createSingleStringPredicate(criteriaBuilder, givenExpression,
                    parameter);

            Optional<Predicate> familyPredicate = createSingleStringPredicate(criteriaBuilder, familyExpression,
                    parameter);

            if (givenPredicate.isEmpty() && familyPredicate.isEmpty()) {
                return Optional.empty();
            }

            if (givenPredicate.isPresent() && familyPredicate.isPresent()) {
                return Optional.of(criteriaBuilder.or(givenPredicate.get(), familyPredicate.get()));
            }

            return givenPredicate.isPresent() ? givenPredicate : familyPredicate;
        });
    }

}