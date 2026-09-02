package org.openelisglobal.common.fhir.internals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Query context that supports both CriteriaQuery and Subquery.
 *
 * @param <T> root entity type
 * @param <R> query result type
 */
public class FhirCriteriaContext<T, R> implements FhirQueryContext<T, R> {

    private static final JoinType DEFAULT_JOIN_TYPE = JoinType.INNER;

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final AbstractQuery<R> criteriaQuery;
    private final Root<T> root;

    private final Map<String, Join<?, ?>> aliases = new LinkedHashMap<>();

    private final List<Predicate> predicates = new ArrayList<>();

    /**
     * Constructor for a main CriteriaQuery.
     */
    public FhirCriteriaContext(EntityManager entityManager, CriteriaBuilder criteriaBuilder,
            CriteriaQuery<R> criteriaQuery, Root<T> root) {

        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager must not be null");

        this.criteriaBuilder = Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        this.criteriaQuery = Objects.requireNonNull(criteriaQuery, "CriteriaQuery must not be null");

        this.root = Objects.requireNonNull(root, "Root must not be null");
    }

    /**
     * Constructor for a Subquery.
     *
     * A subquery does not need an EntityManager because it is executed as part of
     * its parent CriteriaQuery.
     */
    public FhirCriteriaContext(CriteriaBuilder criteriaBuilder, Subquery<R> subquery, Root<T> root) {

        this.entityManager = null;

        this.criteriaBuilder = Objects.requireNonNull(criteriaBuilder, "CriteriaBuilder must not be null");

        this.criteriaQuery = Objects.requireNonNull(subquery, "Subquery must not be null");

        this.root = Objects.requireNonNull(root, "Subquery root must not be null");
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    @Override
    public Root<T> getRoot() {
        return root;
    }

    @Override
    public AbstractQuery<R> getCriteriaQuery() {
        return criteriaQuery;
    }

    @Override
    public Map<String, Join<?, ?>> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    public List<Predicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    @Override
    public void addPredicate(Predicate predicate) {
        if (predicate != null) {
            predicates.add(predicate);
        }
    }

    public void addPredicates(Predicate... predicates) {
        if (predicates == null) {
            return;
        }

        for (Predicate predicate : predicates) {
            addPredicate(predicate);
        }
    }

    @Override
    public Predicate buildPredicate() {
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }

    public FhirCriteriaContext<T, R> applyPredicates() {
        if (!predicates.isEmpty()) {
            criteriaQuery.where(buildPredicate());
        }

        return this;
    }

    @Override
    public Join<?, ?> addJoin(String attributeName, String alias) {

        return addJoin(root, attributeName, alias, DEFAULT_JOIN_TYPE);
    }

    @Override
    public Join<?, ?> addJoin(String attributeName, String alias, JoinType joinType) {

        return addJoin(root, attributeName, alias, joinType);
    }

    @Override
    public Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias) {

        return addJoin(from, attributeName, alias, DEFAULT_JOIN_TYPE);
    }

    @Override
    public Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias, JoinType joinType) {

        Objects.requireNonNull(from, "Join source must not be null");

        Objects.requireNonNull(joinType, "Join type must not be null");

        validateText(attributeName, "Join attribute");
        validateText(alias, "Join alias");

        Join<?, ?> existingJoin = aliases.get(alias);

        if (existingJoin != null) {
            return existingJoin;
        }

        Join<?, ?> join;

        try {
            join = from.join(attributeName, joinType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unable to create " + joinType + " join for attribute '" + attributeName
                    + "' with alias '" + alias + "'", exception);
        }

        join.alias(alias);
        aliases.put(alias, join);

        return join;
    }

    @Override
    public Join<?, ?> getJoin(String alias) {
        validateText(alias, "Join alias");

        Join<?, ?> join = aliases.get(alias);

        if (join == null) {
            throw new IllegalArgumentException(
                    "No join registered with alias '" + alias + "'. Available aliases: " + aliases.keySet());
        }

        return join;
    }

    @Override
    public boolean hasJoin(String alias) {
        return alias != null && !alias.isBlank() && aliases.containsKey(alias);
    }

    /**
     * Returns true when this context wraps a main CriteriaQuery.
     */
    public boolean isMainQuery() {
        return criteriaQuery instanceof CriteriaQuery<?>;
    }

    /**
     * Returns true when this context wraps a Subquery.
     */
    public boolean isSubquery() {
        return criteriaQuery instanceof Subquery<?>;
    }

    /**
     * Returns the underlying main CriteriaQuery.
     */
    @SuppressWarnings("unchecked")
    public CriteriaQuery<R> getMainQuery() {
        if (!(criteriaQuery instanceof CriteriaQuery<?>)) {
            throw new IllegalStateException("This context represents a subquery, not a main query");
        }

        return (CriteriaQuery<R>) criteriaQuery;
    }

    /**
     * Returns the underlying Subquery.
     */
    @SuppressWarnings("unchecked")
    public Subquery<R> getSubquery() {
        if (!(criteriaQuery instanceof Subquery<?>)) {
            throw new IllegalStateException("This context represents a main query, not a subquery");
        }

        return (Subquery<R>) criteriaQuery;
    }

    /**
     * Creates an executable query.
     *
     * Subqueries cannot be executed independently.
     */
    public TypedQuery<R> createQuery() {
        if (entityManager == null || !isMainQuery()) {
            throw new IllegalStateException("A subquery cannot be executed independently");
        }

        applyPredicates();

        return entityManager.createQuery(getMainQuery());
    }

    public FhirCriteriaContext<T, R> distinct(boolean distinct) {
        criteriaQuery.distinct(distinct);
        return this;
    }

    private void validateText(String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    public int predicateCount() {
        return predicates.size();
    }
}
