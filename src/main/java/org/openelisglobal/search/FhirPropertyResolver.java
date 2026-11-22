package org.openelisglobal.search;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Objects;
import org.openelisglobal.common.fhir.internals.FhirQueryContext;
import org.springframework.stereotype.Component;

/**
 * Resolves dotted entity property paths and automatically creates or reuses
 * Criteria API joins.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * person.firstName
 * person.address.city
 * analysis.test.name
 * organization.parent.name
 * </pre>
 */
@Component
public class FhirPropertyResolver {

    private static final JoinType DEFAULT_JOIN_TYPE = JoinType.LEFT;

    /**
     * Resolves a dotted property path using LEFT joins for intermediate
     * relationships.
     */
    public Path<?> resolve(FhirQueryContext<?, ?> context, String propertyPath) {

        return resolve(context, propertyPath, DEFAULT_JOIN_TYPE);
    }

    /**
     * Resolves a dotted property path.
     */
    public Path<?> resolve(FhirQueryContext<?, ?> context, String propertyPath, JoinType joinType) {

        Objects.requireNonNull(context, "FHIR query context must not be null");

        Objects.requireNonNull(joinType, "Join type must not be null");

        validatePropertyPath(propertyPath);

        String[] segments = propertyPath.trim().split("\\.");

        Path<?> currentPath = context.getRoot();

        From<?, ?> currentFrom = context.getRoot();

        StringBuilder aliasBuilder = new StringBuilder();

        for (int index = 0; index < segments.length; index++) {

            String segment = segments[index].trim();

            boolean finalSegment = index == segments.length - 1;

            if (finalSegment) {
                Path<?> result = currentPath.get(segment);

                Objects.requireNonNull(result, "Resolved Criteria path must not be null for property: " + propertyPath);

                return result;
            }

            /*
             * A join can only be created when the current path is a From. Embedded/basic
             * paths must be traversed using Path#get.
             */
            if (currentPath instanceof From<?, ?> from && isAssociation(from, segment)) {

                appendAliasSegment(aliasBuilder, segment);

                String alias = aliasBuilder.toString();

                Join<?, ?> join = getOrCreateJoin(context, from, segment, alias, joinType);

                currentPath = join;
                currentFrom = join;

            } else {

                currentPath = currentPath.get(segment);

                /*
                 * This is normally false for embedded paths, but preserve the From reference if
                 * the provider returns one.
                 */
                if (currentPath instanceof From<?, ?> from) {
                    currentFrom = from;
                }
            }
        }

        return currentPath;
    }

    /**
     * Resolves a property path and validates its mapped Java type.
     *
     * <p>
     * The mapped Path is returned directly. Path.as(...) is intentionally not used
     * because it can introduce an unnecessary Hibernate function/cast expression.
     * </p>
     */
    @SuppressWarnings("unchecked")
    public <Y> Path<Y> resolvePath(FhirQueryContext<?, ?> context, String propertyPath, Class<Y> javaType) {

        Objects.requireNonNull(javaType, "Requested Java type must not be null");

        Path<?> path = resolve(context, propertyPath);

        Class<?> actualType = path.getJavaType();

        if (actualType == null) {
            throw new IllegalArgumentException(
                    "Could not determine Java type for property path '" + propertyPath + "'");
        }

        if (!javaType.isAssignableFrom(actualType)) {
            throw new IllegalArgumentException("Property path '" + propertyPath + "' has Java type "
                    + actualType.getName() + ", not " + javaType.getName());
        }

        return (Path<Y>) path;
    }

    /**
     * Resolves a typed property path without introducing a Criteria cast.
     */
    public <Y> Path<Y> resolve(FhirQueryContext<?, ?> context, String propertyPath, Class<Y> javaType) {

        return resolvePath(context, propertyPath, javaType);
    }

    private Join<?, ?> getOrCreateJoin(FhirQueryContext<?, ?> context, From<?, ?> from, String attributeName,
            String alias, JoinType joinType) {

        if (context.hasJoin(alias)) {
            return context.getJoin(alias);
        }

        return context.addJoin(from, attributeName, alias, joinType);
    }

    /**
     * Determines whether an attribute represents an entity relationship.
     */
    private boolean isAssociation(From<?, ?> from, String attributeName) {

        if (!(from.getModel() instanceof ManagedType<?> managedType)) {
            return false;
        }

        Attribute<?, ?> attribute;

        try {
            attribute = managedType.getAttribute(attributeName);

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Property '" + attributeName + "' does not exist on entity "
                    + managedType.getJavaType().getSimpleName(), exception);
        }

        if (attribute instanceof PluralAttribute<?, ?, ?>) {
            return true;
        }

        if (attribute instanceof SingularAttribute<?, ?> singularAttribute) {
            Attribute.PersistentAttributeType attributeType = singularAttribute.getPersistentAttributeType();

            return attributeType == Attribute.PersistentAttributeType.ONE_TO_ONE
                    || attributeType == Attribute.PersistentAttributeType.MANY_TO_ONE
                    || attributeType == Attribute.PersistentAttributeType.ONE_TO_MANY
                    || attributeType == Attribute.PersistentAttributeType.MANY_TO_MANY;
        }

        return false;
    }

    private void appendAliasSegment(StringBuilder aliasBuilder, String segment) {

        if (!aliasBuilder.isEmpty()) {
            aliasBuilder.append('_');
        }

        aliasBuilder.append(segment);
    }

    private void validatePropertyPath(String propertyPath) {

        if (propertyPath == null || propertyPath.isBlank()) {

            throw new IllegalArgumentException("Property path must not be null or blank");
        }

        String[] segments = propertyPath.trim().split("\\.");

        for (String segment : segments) {
            if (segment.isBlank()) {
                throw new IllegalArgumentException("Invalid property path: " + propertyPath);
            }
        }
    }
}