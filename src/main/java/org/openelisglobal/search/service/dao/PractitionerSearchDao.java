package org.openelisglobal.search.service.dao;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
     * Handles FHIR Practitioner _id as a UUID-backed entity property.
     */
    private <R> Optional<Predicate> createIdPredicate(FhirCriteriaContext<Provider, R> context, TokenAndListParam id) {

        if (id == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<UUID> idExpression = Objects.requireNonNull(
                propertyResolver.resolve(context, FhirConstants.ID_PROPERTY, UUID.class),
                "Property resolver returned null for UUID path: " + FhirConstants.ID_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, id,
                token -> createUuidTokenPredicate(criteriaBuilder, idExpression, token));
    }

    /**
     * Handles FHIR Practitioner identifier as a UUID-backed entity property.
     */
    private <R> Optional<Predicate> createIdentifierPredicate(FhirCriteriaContext<Provider, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<UUID> identifierExpression = Objects.requireNonNull(
                propertyResolver.resolve(context, FhirConstants.IDENTIFIER_SEARCH_HANDLER, UUID.class),
                "Property resolver returned null for UUID path: " + FhirConstants.IDENTIFIER_SEARCH_HANDLER);

        return handleTokenAndListParam(criteriaBuilder, identifier,
                token -> createUuidTokenPredicate(criteriaBuilder, identifierExpression, token));
    }

    /**
     * FHIR Practitioner.name searches across first name, last name, and the
     * concatenated full name.
     */
    private <R> Optional<Predicate> createNamePredicate(FhirCriteriaContext<Provider, R> context,
            StringAndListParam name) {

        if (name == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<String> firstNameExpression = resolveStringExpression(context,
                FhirConstants.FIRST_NAME_SEARCH_HANDLER);

        Expression<String> lastNameExpression = resolveStringExpression(context,
                FhirConstants.LAST_NAME_SEARCH_HANDLER);

        Expression<String> fullNameExpression = criteriaBuilder.concat(criteriaBuilder.concat(firstNameExpression, " "),
                lastNameExpression);

        return handleStringAndListParam(criteriaBuilder, name, parameter -> createFullNamePredicate(criteriaBuilder,
                firstNameExpression, lastNameExpression, fullNameExpression, parameter));
    }

    private Optional<Predicate> createFullNamePredicate(CriteriaBuilder criteriaBuilder,
            Expression<String> firstNameExpression, Expression<String> lastNameExpression,
            Expression<String> fullNameExpression, StringParam parameter) {

        Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        Objects.requireNonNull(firstNameExpression, "First-name Criteria expression must not be null");

        Objects.requireNonNull(lastNameExpression, "Last-name Criteria expression must not be null");

        Objects.requireNonNull(fullNameExpression, "Full-name Criteria expression must not be null");

        if (parameter == null || parameter.getValue() == null || parameter.getValue().isBlank()) {
            return Optional.empty();
        }

        String value = parameter.getValue().trim();

        if (parameter.isExact()) {
            Predicate firstNamePredicate = criteriaBuilder.equal(firstNameExpression, value);

            Predicate lastNamePredicate = criteriaBuilder.equal(lastNameExpression, value);

            Predicate fullNamePredicate = criteriaBuilder.equal(fullNameExpression, value);

            return Optional.of(criteriaBuilder.or(firstNamePredicate, lastNamePredicate, fullNamePredicate));
        }

        String normalizedValue = escapeLikeValue(value.toLowerCase(Locale.ROOT));

        String pattern = parameter.isContains() ? "%" + normalizedValue + "%" : normalizedValue + "%";

        Predicate firstNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(firstNameExpression), pattern);

        Predicate lastNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(lastNameExpression), pattern);

        Predicate fullNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(fullNameExpression), pattern);

        return Optional.of(criteriaBuilder.or(firstNamePredicate, lastNamePredicate, fullNamePredicate));
    }

    private <R> Optional<Predicate> createStringPredicate(FhirCriteriaContext<Provider, R> context, String propertyPath,
            StringAndListParam parameter) {

        if (parameter == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<String> expression = resolveStringExpression(context, propertyPath);

        return handleStringAndListParam(criteriaBuilder, parameter,
                value -> createSingleStringPredicate(criteriaBuilder, expression, value));
    }

    private Optional<Predicate> createSingleStringPredicate(CriteriaBuilder criteriaBuilder,
            Expression<String> expression, StringParam parameter) {

        Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        Objects.requireNonNull(expression, "String Criteria expression must not be null");

        if (parameter == null || parameter.getValue() == null || parameter.getValue().isBlank()) {
            return Optional.empty();
        }

        String value = parameter.getValue().trim();

        if (parameter.isExact()) {
            return Optional.of(criteriaBuilder.equal(expression, value));
        }

        Expression<String> normalizedExpression = criteriaBuilder.lower(expression);

        String normalizedValue = escapeLikeValue(value.toLowerCase(Locale.ROOT));

        if (parameter.isContains()) {
            return Optional.of(criteriaBuilder.like(normalizedExpression, "%" + normalizedValue + "%"));
        }

        return Optional.of(criteriaBuilder.like(normalizedExpression, normalizedValue + "%"));
    }

    /**
     * Builds a predicate for UUID-backed FHIR token parameters.
     *
     * A token system, when present, is currently ignored because the backing
     * OpenELIS property stores only the UUID value.
     */
    private Optional<Predicate> createUuidTokenPredicate(CriteriaBuilder criteriaBuilder, Expression<UUID> expression,
            TokenParam token) {

        Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        Objects.requireNonNull(expression, "UUID Criteria expression must not be null");

        if (token == null || token.getValue() == null || token.getValue().isBlank()) {
            return Optional.empty();
        }

        String value = token.getValue().trim();

        try {
            UUID uuid = UUID.fromString(value);

            return Optional.of(criteriaBuilder.equal(expression, uuid));
        } catch (IllegalArgumentException exception) {
            /*
             * A non-UUID value cannot match a UUID-backed database column. Returning an
             * always-false predicate prevents an invalid token from being silently ignored.
             */
            return Optional.of(criteriaBuilder.disjunction());
        }
    }

    /**
     * Retained for token search parameters whose entity property is a String.
     */
    @SuppressWarnings("unused")
    private Optional<Predicate> createTokenValuePredicate(CriteriaBuilder criteriaBuilder,
            Expression<String> expression, TokenParam token) {

        Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        Objects.requireNonNull(expression, "Token Criteria expression must not be null");

        if (token == null || token.getValue() == null || token.getValue().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(criteriaBuilder.equal(expression, token.getValue().trim()));
    }

    private <R> Optional<Predicate> createLastUpdatedPredicate(FhirCriteriaContext<Provider, R> context,
            DateRangeParam lastUpdated) {

        if (lastUpdated == null) {
            return Optional.empty();
        }

        DateParam lowerBound = lastUpdated.getLowerBound();

        DateParam upperBound = lastUpdated.getUpperBound();

        if (!hasDateValue(lowerBound) && !hasDateValue(upperBound)) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<Date> lastUpdatedExpression = Objects.requireNonNull(
                propertyResolver.resolve(context, FhirConstants.LAST_UPDATED_PROPERTY, Date.class),
                "Property resolver returned null for date path: " + FhirConstants.LAST_UPDATED_PROPERTY);

        Predicate lowerPredicate = createLowerDatePredicate(criteriaBuilder, lastUpdatedExpression, lowerBound);

        Predicate upperPredicate = createUpperDatePredicate(criteriaBuilder, lastUpdatedExpression, upperBound);

        if (lowerPredicate == null) {
            return Optional.ofNullable(upperPredicate);
        }

        if (upperPredicate == null) {
            return Optional.of(lowerPredicate);
        }

        return Optional.of(criteriaBuilder.and(lowerPredicate, upperPredicate));
    }

    private Predicate createLowerDatePredicate(CriteriaBuilder criteriaBuilder, Expression<Date> expression,
            DateParam parameter) {

        if (!hasDateValue(parameter)) {
            return null;
        }

        Date value = parameter.getValue();

        ParamPrefixEnum prefix = parameter.getPrefix();

        if (prefix == ParamPrefixEnum.GREATERTHAN) {
            return criteriaBuilder.greaterThan(expression, value);
        }

        return criteriaBuilder.greaterThanOrEqualTo(expression, value);
    }

    private Predicate createUpperDatePredicate(CriteriaBuilder criteriaBuilder, Expression<Date> expression,
            DateParam parameter) {

        if (!hasDateValue(parameter)) {
            return null;
        }

        Date value = parameter.getValue();

        ParamPrefixEnum prefix = parameter.getPrefix();

        if (prefix == ParamPrefixEnum.LESSTHAN) {
            return criteriaBuilder.lessThan(expression, value);
        }

        return criteriaBuilder.lessThanOrEqualTo(expression, value);
    }

    private Optional<Predicate> handleTokenAndListParam(CriteriaBuilder criteriaBuilder, TokenAndListParam parameters,
            Function<TokenParam, Optional<Predicate>> handler) {

        if (parameters == null || handler == null) {
            return Optional.empty();
        }

        Predicate[] andPredicates = parameters.getValuesAsQueryTokens().stream().map(orList -> {
            Predicate[] orPredicates = orList.getValuesAsQueryTokens().stream().map(handler).flatMap(Optional::stream)
                    .toArray(Predicate[]::new);

            if (orPredicates.length == 0) {
                return Optional.<Predicate>empty();
            }

            if (orPredicates.length == 1) {
                return Optional.of(orPredicates[0]);
            }

            return Optional.of(criteriaBuilder.or(orPredicates));
        }).flatMap(Optional::stream).toArray(Predicate[]::new);

        if (andPredicates.length == 0) {
            return Optional.empty();
        }

        if (andPredicates.length == 1) {
            return Optional.of(andPredicates[0]);
        }

        return Optional.of(criteriaBuilder.and(andPredicates));
    }

    private Optional<Predicate> handleStringAndListParam(CriteriaBuilder criteriaBuilder, StringAndListParam parameters,
            Function<StringParam, Optional<Predicate>> handler) {

        if (parameters == null || handler == null) {
            return Optional.empty();
        }

        Predicate[] andPredicates = parameters.getValuesAsQueryTokens().stream()
                .map(orList -> handleStringOrListParam(criteriaBuilder, orList, handler)).flatMap(Optional::stream)
                .toArray(Predicate[]::new);

        if (andPredicates.length == 0) {
            return Optional.empty();
        }

        if (andPredicates.length == 1) {
            return Optional.of(andPredicates[0]);
        }

        return Optional.of(criteriaBuilder.and(andPredicates));
    }

    private Optional<Predicate> handleStringOrListParam(CriteriaBuilder criteriaBuilder, StringOrListParam parameters,
            Function<StringParam, Optional<Predicate>> handler) {

        if (parameters == null || handler == null) {
            return Optional.empty();
        }

        Predicate[] orPredicates = parameters.getValuesAsQueryTokens().stream().map(handler).flatMap(Optional::stream)
                .toArray(Predicate[]::new);

        if (orPredicates.length == 0) {
            return Optional.empty();
        }

        if (orPredicates.length == 1) {
            return Optional.of(orPredicates[0]);
        }

        return Optional.of(criteriaBuilder.or(orPredicates));
    }

    private <R> CriteriaBuilder requireCriteriaBuilder(FhirCriteriaContext<Provider, R> context) {

        Objects.requireNonNull(context, "FHIR Criteria context must not be null");

        return Objects.requireNonNull(context.getCriteriaBuilder(), "CriteriaBuilder must not be null");
    }

    private <R> Expression<String> resolveStringExpression(FhirCriteriaContext<Provider, R> context,
            String propertyPath) {

        Objects.requireNonNull(propertyPath, "Property path must not be null");

        return Objects.requireNonNull(propertyResolver.resolve(context, propertyPath, String.class),
                "Property resolver returned null for String path: " + propertyPath);
    }

    private boolean hasDateValue(DateParam parameter) {
        return parameter != null && parameter.getValue() != null;
    }

    private String escapeLikeValue(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}