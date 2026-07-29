package org.openelisglobal.common.fhir.dao;

import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterOr;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.common.fhir.internals.FhirQueryContext;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.search.FhirPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base DAO for building and executing FHIR searches using the JPA Criteria API.
 *
 * <p>
 * This class provides a comprehensive framework for converting FHIR search
 * parameters into JPA Criteria queries with proper security, performance, and
 * error handling.
 * </p>
 *
 * <p>
 * The root type represents the OpenELIS database entity being searched, while
 * the result type represents the value returned by the query. The result may be
 * the root entity itself, a DTO projection, a scalar value, or a count.
 * </p>
 *
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe when used with properly
 * configured EntityManager instances.
 * </p>
 *
 * @author OpenELIS Team
 * @version 2.0
 */
@Transactional(readOnly = true)
public abstract class BaseFhirDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseFhirDao.class);

    private static final Locale NORMALIZATION_LOCALE = Locale.ROOT;
    private static final char LIKE_ESCAPE_CHARACTER = '\\';
    private static final int DEFAULT_MAX_RESULTS = Integer
            .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

    @PersistenceContext
    protected EntityManager entityManager;

    protected final FhirPropertyResolver propertyResolver;

    /**
     * Constructs a BaseFhirDao with the required property resolver.
     *
     * @param propertyResolver resolver for mapping FHIR properties to database
     *                         fields
     * @throws NullPointerException if propertyResolver is null
     */
    protected BaseFhirDao(FhirPropertyResolver propertyResolver) {
        this.propertyResolver = Objects.requireNonNull(propertyResolver, "FhirPropertyResolver must not be null");
    }

    /**
     * Creates a Criteria context that returns the root entity.
     *
     * @param rootType entity class used as the query root
     * @param <T>      entity and result type
     * @return initialized Criteria context
     * @throws IllegalArgumentException if rootType is null
     */
    protected <T> FhirCriteriaContext<T, T> createCriteriaContext(Class<T> rootType) {
        Objects.requireNonNull(rootType, "Root type must not be null");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(rootType);
        Root<T> root = criteriaQuery.from(rootType);
        criteriaQuery.select(root);

        LOGGER.debug("Created Criteria context for entity type: {}", rootType.getSimpleName());
        return new FhirCriteriaContext<>(entityManager, criteriaBuilder, criteriaQuery, root);
    }

    /**
     * Creates a Criteria context that returns a projection or scalar result.
     *
     * <p>
     * The caller must configure the query selection before execution:
     * </p>
     *
     * <pre>
     * context.getCriteriaQuery().select(context.getRoot().get("name"));
     * </pre>
     *
     * @param rootType   entity class used as the query root
     * @param resultType result or projection class
     * @param <T>        root entity type
     * @param <R>        query result type
     * @return initialized Criteria context
     * @throws IllegalArgumentException if rootType or resultType is null
     */
    protected <T, R> FhirCriteriaContext<T, R> createCriteriaContext(Class<T> rootType, Class<R> resultType) {
        Objects.requireNonNull(rootType, "Root type must not be null");
        Objects.requireNonNull(resultType, "Result type must not be null");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<R> criteriaQuery = criteriaBuilder.createQuery(resultType);
        Root<T> root = criteriaQuery.from(rootType);

        LOGGER.debug("Created Criteria context for entity type: {} with result type: {}", rootType.getSimpleName(),
                resultType.getSimpleName());
        return new FhirCriteriaContext<>(entityManager, criteriaBuilder, criteriaQuery, root);
    }

    /**
     * Creates a subquery attached to an existing main Criteria query.
     *
     * @param context    parent Criteria query context
     * @param rootType   subquery root entity type
     * @param resultType subquery result type
     * @param <T>        subquery root type
     * @param <R>        subquery result type
     * @return initialized FHIR subquery context
     * @throws IllegalArgumentException if any parameter is null
     */
    protected <T, R> FhirCriteriaContext<T, R> createSubQuery(FhirCriteriaContext<?, ?> parentContext,
            Class<T> rootType, Class<R> resultType) {

        Objects.requireNonNull(parentContext, "Parent Criteria context must not be null");
        Objects.requireNonNull(rootType, "Subquery root type must not be null");
        Objects.requireNonNull(resultType, "Subquery result type must not be null");

        Subquery<R> subquery = parentContext.getCriteriaQuery().subquery(resultType);
        Root<T> root = subquery.from(rootType);

        LOGGER.debug("Created subquery context for entity type: {} with result type: {}", rootType.getSimpleName(),
                resultType.getSimpleName());
        return new FhirCriteriaContext<>(parentContext.getCriteriaBuilder(), subquery, root);
    }

    /**
     * Returns either the query root or a previously registered join.
     *
     * @param context active FHIR query context
     * @param alias   join alias; null or blank means use the root
     * @return root or matching join
     * @throws IllegalArgumentException if context is null
     */
    protected From<?, ?> getRootOrJoin(FhirQueryContext<?, ?> context, String alias) {
        Objects.requireNonNull(context, "FHIR query context must not be null");

        if (alias == null || alias.isBlank()) {
            return context.getRoot();
        }

        From<?, ?> join = context.getJoin(alias);
        if (join == null) {
            LOGGER.warn("Join with alias '{}' not found, falling back to root", alias);
            return context.getRoot();
        }

        return join;
    }

    /**
     * Converts optional predicates into a Predicate array.
     */
    @SafeVarargs
    protected final Predicate[] toPredicateArray(Optional<? extends Predicate>... predicates) {
        if (predicates == null || predicates.length == 0) {
            return new Predicate[0];
        }
        return toPredicateArray(Arrays.stream(predicates));
    }

    /**
     * Converts a collection of optional predicates into a Predicate array.
     */
    protected Predicate[] toPredicateArray(Collection<Optional<? extends Predicate>> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return new Predicate[0];
        }
        return toPredicateArray(predicates.stream());
    }

    /**
     * Converts a stream of optional predicates into a Predicate array.
     */
    protected Predicate[] toPredicateArray(Stream<Optional<? extends Predicate>> predicates) {
        if (predicates == null) {
            return new Predicate[0];
        }
        return predicates.filter(Optional::isPresent).map(Optional::get).toArray(Predicate[]::new);
    }

    /**
     * Combines multiple predicates with AND logic.
     */
    protected Optional<Predicate> combineWithAnd(CriteriaBuilder criteriaBuilder,
            Collection<? extends Predicate> predicates) {
        if (criteriaBuilder == null || predicates == null || predicates.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicateArray = predicates.stream().filter(Objects::nonNull).toArray(Predicate[]::new);

        if (predicateArray.length == 0) {
            return Optional.empty();
        }

        if (predicateArray.length == 1) {
            return Optional.of(predicateArray[0]);
        }

        return Optional.of(criteriaBuilder.and(predicateArray));
    }

    /**
     * Combines multiple predicates with OR logic.
     */
    protected Optional<Predicate> combineWithOr(CriteriaBuilder criteriaBuilder,
            Collection<? extends Predicate> predicates) {
        if (criteriaBuilder == null || predicates == null || predicates.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicateArray = predicates.stream().filter(Objects::nonNull).toArray(Predicate[]::new);

        if (predicateArray.length == 0) {
            return Optional.empty();
        }

        if (predicateArray.length == 1) {
            return Optional.of(predicateArray[0]);
        }

        return Optional.of(criteriaBuilder.or(predicateArray));
    }

    /**
     * Processes repeated FHIR parameters using AND semantics.
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * identifier=value1&amp;identifier=value2
     * </pre>
     *
     * Each repeated parameter group is combined with AND. Values inside one
     * comma-separated group are handled with OR.
     */
    protected <OR extends IQueryParameterOr<P>, P extends IQueryParameterType> Optional<Predicate> handleAndListParam(
            CriteriaBuilder criteriaBuilder, IQueryParameterAnd<OR> params, Function<P, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || params == null || handler == null) {
            return Optional.empty();
        }

        List<OR> orValues = params.getValuesAsQueryTokens();
        if (orValues == null || orValues.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicates = orValues.stream().map(or -> handleOrListParam(criteriaBuilder, or, handler))
                .flatMap(Optional::stream).toArray(Predicate[]::new);

        if (predicates.length == 0) {
            return Optional.empty();
        }

        if (predicates.length == 1) {
            return Optional.of(predicates[0]);
        }

        return Optional.of(criteriaBuilder.and(predicates));
    }

    /**
     * Processes comma-separated FHIR parameter values using OR semantics.
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * status=final,preliminary
     * </pre>
     */
    protected <P extends IQueryParameterType> Optional<Predicate> handleOrListParam(CriteriaBuilder criteriaBuilder,
            IQueryParameterOr<P> params, Function<P, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || params == null || handler == null) {
            return Optional.empty();
        }

        List<P> values = params.getValuesAsQueryTokens();
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicates = values.stream().map(handler).flatMap(Optional::stream).toArray(Predicate[]::new);

        if (predicates.length == 0) {
            return Optional.empty();
        }

        if (predicates.length == 1) {
            return Optional.of(predicates[0]);
        }

        return Optional.of(criteriaBuilder.or(predicates));
    }

    /**
     * Processes repeated token parameters using AND semantics while grouping the
     * token values in each OR group by token system.
     */
    protected <OR extends IQueryParameterOr<TokenParam>> Optional<Predicate> handleAndListParamBySystem(
            CriteriaBuilder criteriaBuilder, IQueryParameterAnd<OR> params,
            BiFunction<String, List<TokenParam>, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || params == null || handler == null) {
            return Optional.empty();
        }

        List<OR> orValues = params.getValuesAsQueryTokens();
        if (orValues == null || orValues.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicates = orValues.stream().map(or -> handleOrListParamBySystem(criteriaBuilder, or, handler))
                .flatMap(Optional::stream).toArray(Predicate[]::new);

        if (predicates.length == 0) {
            return Optional.empty();
        }

        if (predicates.length == 1) {
            return Optional.of(predicates[0]);
        }

        return Optional.of(criteriaBuilder.and(predicates));
    }

    /**
     * Groups token parameters by system and combines each system group using OR
     * semantics.
     */
    protected Optional<Predicate> handleOrListParamBySystem(CriteriaBuilder criteriaBuilder,
            IQueryParameterOr<TokenParam> params, BiFunction<String, List<TokenParam>, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || params == null || handler == null) {
            return Optional.empty();
        }

        List<TokenParam> values = params.getValuesAsQueryTokens();
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] predicates = values.stream().filter(Objects::nonNull)
                .collect(Collectors.groupingBy(token -> normalizeSystem(token.getSystem()))).entrySet().stream()
                .map(entry -> handler.apply(entry.getKey(), entry.getValue())).flatMap(Optional::stream)
                .toArray(Predicate[]::new);

        if (predicates.length == 0) {
            return Optional.empty();
        }

        if (predicates.length == 1) {
            return Optional.of(predicates[0]);
        }

        return Optional.of(criteriaBuilder.or(predicates));
    }

    /**
     * Processes token AND list parameters.
     */
    protected Optional<Predicate> handleTokenAndListParam(CriteriaBuilder criteriaBuilder, TokenAndListParam parameters,
            Function<TokenParam, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || parameters == null || handler == null) {
            return Optional.empty();
        }

        List<TokenOrListParam> andValues = parameters.getValuesAsQueryTokens();

        if (andValues == null || andValues.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] andPredicates = andValues.stream().filter(Objects::nonNull).map(orList -> {

            List<TokenParam> orValues = orList.getValuesAsQueryTokens();

            if (orValues == null || orValues.isEmpty()) {
                return Optional.<Predicate>empty();
            }

            Predicate[] orPredicates = orValues.stream().filter(Objects::nonNull).map(handler).filter(Objects::nonNull)
                    .flatMap(Optional::stream).toArray(Predicate[]::new);

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

    /**
     * Processes string AND list parameters.
     */
    protected Optional<Predicate> handleStringAndListParam(CriteriaBuilder criteriaBuilder,
            StringAndListParam parameters, Function<StringParam, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || parameters == null || handler == null) {
            return Optional.empty();
        }

        List<StringOrListParam> andValues = parameters.getValuesAsQueryTokens();
        if (andValues == null || andValues.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] andPredicates = andValues.stream()
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

    /**
     * Processes string OR list parameters.
     */
    protected Optional<Predicate> handleStringOrListParam(CriteriaBuilder criteriaBuilder, StringOrListParam parameters,
            Function<StringParam, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || parameters == null || handler == null) {
            return Optional.empty();
        }

        List<StringParam> values = parameters.getValuesAsQueryTokens();
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }

        Predicate[] orPredicates = values.stream().map(handler).flatMap(Optional::stream).toArray(Predicate[]::new);

        if (orPredicates.length == 0) {
            return Optional.empty();
        }

        if (orPredicates.length == 1) {
            return Optional.of(orPredicates[0]);
        }

        return Optional.of(criteriaBuilder.or(orPredicates));
    }

    /**
     * Converts TokenParams into a list of non-null token values.
     */
    protected List<String> tokensToList(List<TokenParam> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokensToStream(tokens).collect(Collectors.toList());
    }

    /**
     * Converts TokenParams into a stream of non-null token values.
     */
    protected Stream<String> tokensToStream(List<TokenParam> tokens) {
        if (tokens == null) {
            return Stream.empty();
        }
        return tokens.stream().filter(Objects::nonNull).map(TokenParam::getValue).filter(Objects::nonNull)
                .map(String::trim).filter(value -> !value.isEmpty());
    }

    /**
     * Creates a string predicate with proper LIKE escaping.
     */
    protected <T, R> Optional<Predicate> createStringPredicate(FhirCriteriaContext<T, R> context, String propertyPath,
            StringAndListParam parameter) {

        if (context == null || parameter == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();
        Expression<String> expression = resolveStringExpression(context, propertyPath);

        return handleStringAndListParam(criteriaBuilder, parameter,
                value -> createSingleStringPredicate(criteriaBuilder, expression, value));
    }

    /**
     * Creates a single string predicate with proper LIKE escaping.
     */
    protected Optional<Predicate> createSingleStringPredicate(CriteriaBuilder criteriaBuilder,
            Expression<String> expression, StringParam parameter) {

        if (criteriaBuilder == null || expression == null || parameter == null) {
            return Optional.empty();
        }

        String value = parameter.getValue();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String trimmedValue = value.trim();
        String escapedValue = escapeLikeValue(trimmedValue.toLowerCase(NORMALIZATION_LOCALE));

        if (parameter.isExact()) {
            return Optional.of(criteriaBuilder.equal(expression, trimmedValue));
        }

        Expression<String> normalizedExpression = criteriaBuilder.lower(expression);

        if (parameter.isContains()) {
            return Optional
                    .of(criteriaBuilder.like(normalizedExpression, "%" + escapedValue + "%", LIKE_ESCAPE_CHARACTER));
        }

        return Optional.of(criteriaBuilder.like(normalizedExpression, escapedValue + "%", LIKE_ESCAPE_CHARACTER));
    }

    /**
     * Creates a token value predicate.
     */
    protected Optional<Predicate> createTokenValuePredicate(CriteriaBuilder criteriaBuilder,
            Expression<String> expression, TokenParam token) {

        if (criteriaBuilder == null || expression == null || token == null) {
            return Optional.empty();
        }

        String value = token.getValue();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(criteriaBuilder.equal(expression, value.trim()));
    }

    /**
     * Creates a UUID token predicate with proper error handling.
     */
    protected Optional<Predicate> createUuidTokenPredicate(CriteriaBuilder criteriaBuilder, Expression<UUID> expression,
            TokenParam token) {

        if (criteriaBuilder == null || expression == null || token == null) {
            return Optional.empty();
        }

        String value = token.getValue();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            UUID uuid = UUID.fromString(value.trim());
            return Optional.of(criteriaBuilder.equal(expression, uuid));
        } catch (IllegalArgumentException exception) {
            LOGGER.debug("Invalid UUID format: '{}', creating false predicate", value);
            // Return a predicate that is always false
            return Optional.of(criteriaBuilder.disjunction());
        }
    }

    /**
     * Creates a value predicate for any type.
     */
    protected <V> Optional<Predicate> createValuePredicate(CriteriaBuilder criteriaBuilder, Expression<V> expression,
            V value) {

        if (criteriaBuilder == null || expression == null || value == null) {
            return Optional.empty();
        }

        return Optional.of(criteriaBuilder.equal(expression, value));
    }

    /**
     * Creates a last updated date range predicate.
     */
    protected <T, R> Optional<Predicate> createLastUpdatedPredicate(FhirCriteriaContext<T, R> context,
            DateRangeParam lastUpdated) {

        if (context == null || lastUpdated == null) {
            return Optional.empty();
        }

        DateParam lowerBound = lastUpdated.getLowerBound();
        DateParam upperBound = lastUpdated.getUpperBound();

        if (!hasDateValue(lowerBound) && !hasDateValue(upperBound)) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();
        Expression<Date> lastUpdatedExpression = resolveExpression(context, FhirConstants.LAST_UPDATED_PROPERTY,
                Date.class);

        Predicate lowerPredicate = createLowerDatePredicate(criteriaBuilder, lastUpdatedExpression, lowerBound);

        Predicate upperPredicate = createUpperDatePredicate(criteriaBuilder, lastUpdatedExpression, upperBound);

        if (lowerPredicate == null && upperPredicate == null) {
            return Optional.empty();
        }

        if (lowerPredicate == null) {
            return Optional.of(upperPredicate);
        }

        if (upperPredicate == null) {
            return Optional.of(lowerPredicate);
        }

        return Optional.of(criteriaBuilder.and(lowerPredicate, upperPredicate));
    }

    /**
     * Creates a lower bound date predicate.
     */
    protected Predicate createLowerDatePredicate(CriteriaBuilder criteriaBuilder, Expression<Date> expression,
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

    /**
     * Creates an upper bound date predicate.
     */
    protected Predicate createUpperDatePredicate(CriteriaBuilder criteriaBuilder, Expression<Date> expression,
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

    /**
     * Adds an optional predicate to the supplied context.
     */
    protected void addPredicate(FhirQueryContext<?, ?> context, Optional<? extends Predicate> predicate) {
        if (context == null || predicate == null) {
            return;
        }
        predicate.ifPresent(context::addPredicate);
    }

    /**
     * Adds multiple predicates to the supplied context.
     */
    @SafeVarargs
    protected final void addPredicates(FhirQueryContext<?, ?> context, Optional<? extends Predicate>... predicates) {
        if (context == null || predicates == null) {
            return;
        }
        for (Optional<? extends Predicate> predicate : predicates) {
            addPredicate(context, predicate);
        }
    }

    /**
     * Executes a Criteria query and returns all matching results.
     */
    protected <R> List<R> list(FhirCriteriaContext<?, R> context) {
        validateContext(context);
        return executeQuery(context, 0, DEFAULT_MAX_RESULTS);
    }

    /**
     * Executes a paginated Criteria query.
     *
     * @param context     query context
     * @param firstResult zero-based result offset
     * @param maxResults  maximum number of results
     * @throws IllegalArgumentException if pagination parameters are invalid
     */
    protected <R> List<R> list(FhirCriteriaContext<?, R> context, int firstResult, int maxResults) {
        validateContext(context);
        validatePagination(firstResult, maxResults);
        return executeQuery(context, firstResult, maxResults);
    }

    /**
     * Executes a Criteria query and returns the first matching result.
     */
    protected <R> Optional<R> single(FhirCriteriaContext<?, R> context) {
        validateContext(context);
        return context.createQuery().setMaxResults(1).getResultStream().findFirst();
    }

    /**
     * Returns whether the query has at least one matching row using an efficient
     * count query.
     */
    protected boolean exists(FhirCriteriaContext<?, ?> context) {
        validateContext(context);

        CriteriaBuilder cb = context.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(context.getRoot()));

        Predicate wherePredicate = context.buildPredicate();
        if (wherePredicate != null) {
            countQuery.where(wherePredicate);
        }

        Long count = entityManager.createQuery(countQuery).getSingleResult();
        LOGGER.debug("Exists check returned: {}", count > 0);
        return count > 0;
    }

    /**
     * Executes a count query.
     */
    protected <T> long count(Class<T> rootType, Consumer<FhirCriteriaContext<T, Long>> predicateConfigurer) {
        Objects.requireNonNull(rootType, "Root type must not be null");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(rootType);

        FhirCriteriaContext<T, Long> context = new FhirCriteriaContext<>(entityManager, criteriaBuilder, criteriaQuery,
                root);

        if (predicateConfigurer != null) {
            predicateConfigurer.accept(context);
        }

        criteriaQuery.select(criteriaBuilder.countDistinct(root));

        Predicate wherePredicate = context.buildPredicate();
        if (wherePredicate != null) {
            criteriaQuery.where(wherePredicate);
        }

        long count = entityManager.createQuery(criteriaQuery).getSingleResult();
        LOGGER.debug("Count query returned: {}", count);
        return count;
    }

    /**
     * Resolves a database property path to a typed Criteria expression.
     */
    protected <T, R, V> Expression<V> resolveExpression(FhirCriteriaContext<T, R> context, String propertyPath,
            Class<V> javaType) {

        Objects.requireNonNull(context, "FHIR Criteria context must not be null");
        Objects.requireNonNull(propertyPath, "Property path must not be null");
        if (propertyPath.isBlank()) {
            throw new IllegalArgumentException("Property path must not be blank");
        }
        Objects.requireNonNull(javaType, "Expression Java type must not be null");

        Expression<V> resolved = propertyResolver.resolve(context, propertyPath, javaType);
        if (resolved == null) {
            String errorMessage = String.format("Failed to resolve property: '%s' of type: %s", propertyPath,
                    javaType.getSimpleName());
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.trace("Resolved expression for property: {} of type: {}", propertyPath, javaType.getSimpleName());
        return resolved;
    }

    /**
     * Resolves a string property expression.
     */
    protected <T, R> Expression<String> resolveStringExpression(FhirCriteriaContext<T, R> context,
            String propertyPath) {
        return resolveExpression(context, propertyPath, String.class);
    }

    /**
     * Resolves a UUID property expression.
     */
    protected <T, R> Expression<UUID> resolveUuidExpression(FhirCriteriaContext<T, R> context, String propertyPath) {
        return resolveExpression(context, propertyPath, UUID.class);
    }

    /**
     * Escapes SQL LIKE wildcards to prevent injection.
     */
    protected String escapeLikeValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("" + LIKE_ESCAPE_CHARACTER, "" + LIKE_ESCAPE_CHARACTER + LIKE_ESCAPE_CHARACTER)
                .replace("%", LIKE_ESCAPE_CHARACTER + "%").replace("_", LIKE_ESCAPE_CHARACTER + "_");
    }

    /**
     * Checks if a DateParam has a value.
     */
    protected boolean hasDateValue(DateParam parameter) {
        return parameter != null && parameter.getValue() != null;
    }

    protected <T, R> Optional<Predicate> createIdentifierPredicate(FhirCriteriaContext<T, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();

        Expression<UUID> identifierExpression = propertyResolver.resolve(context,
                FhirConstants.IDENTIFIER_SEARCH_HANDLER, UUID.class);

        return handleTokenAndListParam(criteriaBuilder, identifier,
                token -> createUuidTokenPredicate(criteriaBuilder, identifierExpression, token));
    }

    protected <T, R> Optional<Predicate> createIdPredicate(FhirCriteriaContext<T, R> context, TokenAndListParam id) {

        if (id == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();

        Expression<UUID> idExpression = propertyResolver.resolve(context, FhirConstants.ID_PROPERTY, UUID.class);

        return handleTokenAndListParam(criteriaBuilder, id,
                token -> createUuidTokenPredicate(criteriaBuilder, idExpression, token));
    }

    /**
     * Normalizes a token system string.
     */
    protected String normalizeSystem(String system) {
        return system == null ? "" : system.trim();
    }

    /**
     * Gets the CriteriaBuilder from the supplied context.
     */
    protected <T, R> CriteriaBuilder requireCriteriaBuilder(FhirCriteriaContext<T, R> context) {
        Objects.requireNonNull(context, "FHIR Criteria context must not be null");

        CriteriaBuilder criteriaBuilder = context.getCriteriaBuilder();
        if (criteriaBuilder == null) {
            throw new IllegalStateException("CriteriaBuilder must not be null in FHIR Criteria context");
        }

        return criteriaBuilder;
    }

    /**
     * Executes a query with pagination parameters.
     */
    private <R> List<R> executeQuery(FhirCriteriaContext<?, R> context, int firstResult, int maxResults) {
        try {
            List<R> results = context.createQuery().setFirstResult(firstResult).setMaxResults(maxResults)
                    .getResultList();

            LOGGER.debug("Query executed successfully, returned {} results", results.size());
            return results;
        } catch (Exception exception) {
            LOGGER.error("Error executing query: {}", exception.getMessage(), exception);
            throw new RuntimeException("Failed to execute FHIR query", exception);
        }
    }

    /**
     * Validates the context.
     */
    private void validateContext(FhirCriteriaContext<?, ?> context) {
        Objects.requireNonNull(context, "FHIR Criteria context must not be null");
    }

    /**
     * Validates pagination parameters.
     */
    private void validatePagination(int firstResult, int maxResults) {
        if (firstResult < 0) {
            throw new IllegalArgumentException("First result must be zero or greater: " + firstResult);
        }
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be greater than zero: " + maxResults);
        }

    }
}