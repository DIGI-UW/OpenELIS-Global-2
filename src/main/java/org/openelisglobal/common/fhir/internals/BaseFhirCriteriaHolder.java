package org.openelisglobal.common.fhir.internals;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseFhirCriteriaHolder<V, T> implements FhirQueryContext<V, T> {

    private final CriteriaBuilder criteriaBuilder;
    private final Root<T> root;
    private final Map<String, Join<?, ?>> aliases = new LinkedHashMap<>();
    private final List<Predicate> predicates = new ArrayList<>();

    protected BaseFhirCriteriaHolder(CriteriaBuilder criteriaBuilder, Root<T> root) {
        this.criteriaBuilder = criteriaBuilder;
        this.root = root;
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    @Override
    public Root getRoot() {
        return root;
    }

    @Override
    public Map<String, Join<?, ?>> getAliases() {
        return aliases;
    }

    public List<Predicate> getPredicates() {
        return predicates;
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

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    @Override
    public Join<?, ?> addJoin(String attributeName, String alias) {
        return addJoin(root, attributeName, alias, JoinType.INNER);
    }

    @Override
    public Join<?, ?> addJoin(String attributeName, String alias, JoinType joinType) {
        return addJoin(root, attributeName, alias, joinType);
    }

    public Join<?, ?> addJoin(String attributeName, String alias, JoinType joinType,
            Function<From<?, ?>, Predicate> onGenerator) {
        return addJoin(root, attributeName, alias, joinType, onGenerator);
    }

    @Override
    public Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias) {
        return addJoin(from, attributeName, alias, JoinType.INNER);
    }

    @Override
    public Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias, JoinType joinType) {
        return addJoin(from, attributeName, alias, joinType, null);
    }

    public Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias, JoinType joinType,
            Function<From<?, ?>, Predicate> onGenerator) {

        Join<?, ?> existing = aliases.get(alias);

        if (existing != null) {

            if (onGenerator != null) {
                Predicate newOn = onGenerator.apply(existing);

                if (newOn != null) {
                    Predicate existingOn = existing.getOn();

                    existing.on(existingOn == null ? newOn : criteriaBuilder.and(existingOn, newOn));
                }
            }

            return existing;
        }

        Join<?, ?> join = from.join(attributeName, joinType);
        join.alias(alias);

        aliases.put(alias, join);

        if (onGenerator != null) {
            Predicate on = onGenerator.apply(join);

            if (on != null) {
                join.on(on);
            }
        }

        return join;
    }

    @Override
    public Join<?, ?> getJoin(String alias) {
        return aliases.get(alias);
    }

    @Override
    public boolean hasJoin(String alias) {
        return aliases.containsKey(alias);
    }

    /**
     * Returns an existing join or throws an exception.
     */
    public Join<?, ?> requireJoin(String alias) {
        Join<?, ?> join = aliases.get(alias);

        if (join == null) {
            throw new IllegalArgumentException("No join registered with alias '" + alias + "'");
        }

        return join;
    }

    /**
     * Removes all predicates.
     */
    public void clearPredicates() {
        predicates.clear();
    }

    /**
     * Removes all joins.
     */
    public void clearJoins() {
        aliases.clear();
    }

    /**
     * Clears the complete state.
     */
    public void clear() {
        clearPredicates();
        clearJoins();
    }
}