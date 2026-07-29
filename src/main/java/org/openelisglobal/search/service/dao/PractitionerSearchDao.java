package org.openelisglobal.search.service.dao;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
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

    public PractitionerSearchDao(FhirPropertyResolver propertyResolver) {

        super(propertyResolver);
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

        /*
         * Searches every ContactPoint-related field.
         */
        addPredicate(context, createTelecomPredicate(context, params.getTelecom()));

        /*
         * Searches only the email field.
         */
        addPredicate(context, createEmailPredicate(context, params.getEmail()));

        /*
         * Searches telephone-related fields, excluding email and fax.
         */
        addPredicate(context, createPhonePredicate(context, params.getPhone()));

        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /**
     * Implements the standard Practitioner.name search.
     *
     * <p>
     * Each supplied value is matched against both the given name and family name.
     * </p>
     *
     * <pre>
     * Practitioner?name=John
     *
     * person.firstName LIKE 'john%'
     * OR
     * person.lastName LIKE 'john%'
     * </pre>
     */
    private <R> Optional<Predicate> createNamePredicate(FhirCriteriaContext<Provider, R> context,
            StringAndListParam name) {

        if (name == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<String> givenExpression = resolveStringExpression(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER);

        Expression<String> familyExpression = resolveStringExpression(context, FhirConstants.LAST_NAME_SEARCH_HANDLER);

        return handleStringAndListParam(criteriaBuilder, name, parameter -> {

            Optional<Predicate> givenPredicate = createSingleStringPredicate(criteriaBuilder, givenExpression,
                    parameter);

            Optional<Predicate> familyPredicate = createSingleStringPredicate(criteriaBuilder, familyExpression,
                    parameter);

            return combineWithOr(criteriaBuilder,
                    Stream.of(givenPredicate, familyPredicate).flatMap(Optional::stream).toList());
        });
    }

    /**
     * Implements the standard Practitioner.telecom token search.
     *
     * <p>
     * Repeated token parameters are combined with AND semantics, while
     * comma-separated token values are combined with OR semantics.
     * </p>
     *
     * <p>
     * The token system may be used as a ContactPoint system selector:
     * {@code phone}, {@code email}, or {@code fax}. When no system is supplied, all
     * supported contact fields are searched.
     * </p>
     */
    private <R> Optional<Predicate> createTelecomPredicate(FhirCriteriaContext<Provider, R> context,
            TokenAndListParam telecom) {

        return createContactPointPredicate(context, telecom,
                List.of(FhirConstants.EMAIL_SEARCH_HANDLER, FhirConstants.WORK_PHONE_SEARCH_HANDLER,
                        FhirConstants.HOME_PHONE_SEARCH_HANDLER, FhirConstants.CELL_PHONE_SEARCH_HANDLER,
                        FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER, FhirConstants.FAX_SEARCH_HANDLER));
    }

    /**
     * Implements the standard Practitioner.email token search.
     */
    private <R> Optional<Predicate> createEmailPredicate(FhirCriteriaContext<Provider, R> context,
            TokenAndListParam email) {

        return createContactPointPredicate(context, email, List.of(FhirConstants.EMAIL_SEARCH_HANDLER));
    }

    /**
     * Implements the standard Practitioner.phone token search.
     *
     * <p>
     * Email and fax fields are excluded.
     * </p>
     */
    private <R> Optional<Predicate> createPhonePredicate(FhirCriteriaContext<Provider, R> context,
            TokenAndListParam phone) {

        return createContactPointPredicate(context, phone,
                List.of(FhirConstants.WORK_PHONE_SEARCH_HANDLER, FhirConstants.HOME_PHONE_SEARCH_HANDLER,
                        FhirConstants.CELL_PHONE_SEARCH_HANDLER, FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER));
    }

    /**
     * Builds a token predicate over one or more OpenELIS contact-point fields.
     *
     * <p>
     * Repeated token parameters are combined with AND, while comma-separated token
     * values are combined with OR.
     * </p>
     */
    private <R> Optional<Predicate> createContactPointPredicate(FhirCriteriaContext<Provider, R> context,
            TokenAndListParam tokenParam, List<String> configuredPropertyPaths) {

        if (tokenParam == null || configuredPropertyPaths == null || configuredPropertyPaths.isEmpty()) {

            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        List<Predicate> andPredicates = new ArrayList<>();

        /*
         * TokenAndListParam:
         *
         * ?telecom=value1&telecom=value2 AND semantics
         *
         * ?telecom=value1,value2 OR semantics
         */
        tokenParam.getValuesAsQueryTokens().forEach(orList -> {

            List<Predicate> orPredicates = new ArrayList<>();

            /*
             * TokenOrListParam is not Iterable, so its token values must be obtained
             * through getValuesAsQueryTokens().
             */
            orList.getValuesAsQueryTokens().forEach(token -> {

                String system = normalize(token.getSystem());

                String value = normalize(token.getValue());

                if (value == null) {
                    return;
                }

                List<String> selectedPropertyPaths = selectContactPointProperties(system, configuredPropertyPaths);

                for (String propertyPath : selectedPropertyPaths) {

                    Expression<String> expression = resolveStringExpression(context, propertyPath);

                    Predicate valuePredicate = criteriaBuilder.equal(criteriaBuilder.lower(expression),
                            value.toLowerCase(Locale.ROOT));

                    orPredicates.add(valuePredicate);
                }
            });

            combineWithOr(criteriaBuilder, orPredicates).ifPresent(andPredicates::add);
        });

        if (andPredicates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(criteriaBuilder.and(andPredicates.toArray(Predicate[]::new)));
    }

    /**
     * Selects contact fields based on the optional ContactPoint system token.
     */
    private List<String> selectContactPointProperties(String system, List<String> configuredPropertyPaths) {

        if (system == null) {
            return configuredPropertyPaths;
        }

        return switch (system.toLowerCase(Locale.ROOT)) {
        case "email" -> configuredPropertyPaths.contains(FhirConstants.EMAIL_SEARCH_HANDLER)
                ? List.of(FhirConstants.EMAIL_SEARCH_HANDLER)
                : List.of();

        case "fax" -> configuredPropertyPaths.contains(FhirConstants.FAX_SEARCH_HANDLER)
                ? List.of(FhirConstants.FAX_SEARCH_HANDLER)
                : List.of();

        case "phone" -> configuredPropertyPaths.stream().filter(this::isPhoneProperty).toList();

        default -> List.of();
        };
    }

    private boolean isPhoneProperty(String propertyPath) {

        return FhirConstants.WORK_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.HOME_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.CELL_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER.equals(propertyPath);
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}