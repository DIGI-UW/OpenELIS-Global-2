package org.openelisglobal.common.fhir.dao;

import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterOr;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.common.fhir.internals.FhirQueryContext;

/**
 * Base DAO for building and executing FHIR searches using the JPA Criteria API.
 *
 * <p>
 * The root type represents the OpenELIS database entity being searched, while
 * the result type represents the value returned by the query. The result may be
 * the root entity itself, a DTO projection, a scalar value, or a count.
 * </p>
 */
public abstract class BaseFhirDao {

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Creates a Criteria context that returns the root entity.
     *
     * @param rootType entity class used as the query root
     * @param <T>      entity and result type
     * @return initialized Criteria context
     */
    protected <T> FhirCriteriaContext<T, T> createCriteriaContext(Class<T> rootType) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(rootType);

        Root<T> root = criteriaQuery.from(rootType);

        /*
         * Explicitly select the root. Although JPA providers commonly infer this
         * selection, setting it here makes the query definition clear.
         */
        criteriaQuery.select(root);

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
     */
    protected <T, R> FhirCriteriaContext<T, R> createCriteriaContext(Class<T> rootType, Class<R> resultType) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<R> criteriaQuery = criteriaBuilder.createQuery(resultType);

        Root<T> root = criteriaQuery.from(rootType);

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
     */
    protected <T, R> FhirCriteriaContext<T, R> createSubQuery(FhirCriteriaContext<?, ?> parentContext,
            Class<T> rootType, Class<R> resultType) {

        if (parentContext == null) {
            throw new IllegalArgumentException("Parent Criteria context must not be null");
        }

        if (rootType == null) {
            throw new IllegalArgumentException("Subquery root type must not be null");
        }

        if (resultType == null) {
            throw new IllegalArgumentException("Subquery result type must not be null");
        }

        Subquery<R> subquery = parentContext.getCriteriaQuery().subquery(resultType);

        Root<T> root = subquery.from(rootType);

        return new FhirCriteriaContext<>(parentContext.getCriteriaBuilder(), subquery, root);
    }

    /**
     * Returns either the query root or a previously registered join.
     *
     * @param context active FHIR query context
     * @param alias   join alias; null or blank means use the root
     * @return root or matching join
     */
    protected From<?, ?> getRootOrJoin(FhirQueryContext<?, ?> context, String alias) {

        if (context == null) {
            throw new IllegalArgumentException("FHIR query context must not be null");
        }

        if (alias == null || alias.isBlank()) {
            return context.getRoot();
        }

        return context.getJoin(alias);
    }

    /**
     * Converts optional predicates into a Predicate array.
     */
    @SafeVarargs
    protected final Predicate[] toPredicateArray(Optional<? extends Predicate>... predicates) {

        if (predicates == null) {
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

        Predicate[] predicates = params.getValuesAsQueryTokens().stream()
                .map(or -> handleOrListParam(criteriaBuilder, or, handler)).flatMap(Optional::stream)
                .toArray(Predicate[]::new);

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

        Predicate[] predicates = params.getValuesAsQueryTokens().stream().map(handler).flatMap(Optional::stream)
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
     * Processes repeated token parameters using AND semantics while grouping the
     * token values in each OR group by token system.
     */
    protected <OR extends IQueryParameterOr<TokenParam>> Optional<Predicate> handleAndListParamBySystem(
            CriteriaBuilder criteriaBuilder, IQueryParameterAnd<OR> params,
            BiFunction<String, List<TokenParam>, Optional<Predicate>> handler) {

        if (criteriaBuilder == null || params == null || handler == null) {

            return Optional.empty();
        }

        Predicate[] predicates = params.getValuesAsQueryTokens().stream()
                .map(or -> handleOrListParamBySystem(criteriaBuilder, or, handler)).flatMap(Optional::stream)
                .toArray(Predicate[]::new);

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

        Predicate[] predicates = params.getValuesAsQueryTokens().stream().filter(token -> token != null)
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

        return tokens.stream().filter(token -> token != null).map(TokenParam::getValue).filter(value -> value != null);
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
     * Executes a Criteria query and returns all matching results.
     */
    protected <R> List<R> list(FhirCriteriaContext<?, R> context) {

        validateContext(context);

        return context.createQuery().getResultList();
    }

    /**
     * Executes a paginated Criteria query.
     *
     * @param context     query context
     * @param firstResult zero-based result offset
     * @param maxResults  maximum number of results
     */
    protected <R> List<R> list(FhirCriteriaContext<?, R> context, int firstResult, int maxResults) {

        validateContext(context);

        if (firstResult < 0) {
            throw new IllegalArgumentException("First result must be zero or greater");
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be greater than zero");
        }

        return context.createQuery().setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    /**
     * Executes a Criteria query and returns the first matching result.
     */
    protected <R> Optional<R> single(FhirCriteriaContext<?, R> context) {

        validateContext(context);

        return context.createQuery().setMaxResults(1).getResultStream().findFirst();
    }

    /**
     * Returns whether the query has at least one matching row.
     */
    protected boolean exists(FhirCriteriaContext<?, ?> context) {

        validateContext(context);

        return !context.createQuery().setMaxResults(1).getResultList().isEmpty();
    }

    private String normalizeSystem(String system) {
        return system == null ? "" : system.trim();
    }

    private void validateContext(FhirCriteriaContext<?, ?> context) {

        if (context == null) {
            throw new IllegalArgumentException("FHIR Criteria context must not be null");
        }
    }

    protected <T> long count(Class<T> rootType,
            java.util.function.Consumer<FhirCriteriaContext<T, Long>> predicateConfigurer) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<T> root = criteriaQuery.from(rootType);

        FhirCriteriaContext<T, Long> context = new FhirCriteriaContext<>(entityManager, criteriaBuilder, criteriaQuery,
                root);

        if (predicateConfigurer != null) {
            predicateConfigurer.accept(context);
        }

        criteriaQuery.select(criteriaBuilder.countDistinct(root));

        if (context.predicateCount() > 0) {
            criteriaQuery.where(context.buildPredicate());
        }

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
}
