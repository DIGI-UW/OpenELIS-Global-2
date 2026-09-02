package org.openelisglobal.common.fhir.internals;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Map;

/**
 * Shared context for both main Criteria queries and subqueries.
 *
 * @param <T> query root entity type
 * @param <R> query result type
 */
public interface FhirQueryContext<T, R> {

    CriteriaBuilder getCriteriaBuilder();

    Root<T> getRoot();

    /**
     * Returns either a CriteriaQuery or a Subquery.
     *
     * Both query types implement AbstractQuery.
     */
    AbstractQuery<R> getCriteriaQuery();

    Map<String, Join<?, ?>> getAliases();

    void addPredicate(Predicate predicate);

    Predicate buildPredicate();

    Join<?, ?> addJoin(String attributeName, String alias);

    Join<?, ?> addJoin(String attributeName, String alias, JoinType joinType);

    Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias);

    Join<?, ?> addJoin(From<?, ?> from, String attributeName, String alias, JoinType joinType);

    Join<?, ?> getJoin(String alias);

    boolean hasJoin(String alias);
}
