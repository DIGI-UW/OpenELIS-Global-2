package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.OrganizationSearchParams;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Organization records with FHIR Organization search
 * parameters. Identifier systems mirror the transform: {@code org_uuid},
 * {@code org_code}, {@code org_shortName} and {@code org_cliaNum}; a bare value
 * is matched against all of them.
 */
@Repository
@Transactional(readOnly = true)
public class OrganizationSearchDao extends BaseFhirDao {

    private static final String NAME_PROPERTY = "organizationName";
    private static final String ACTIVE_PROPERTY = "isActive";
    private static final String CODE_PROPERTY = "code";
    private static final String SHORT_NAME_PROPERTY = "shortName";
    private static final String CLIA_PROPERTY = "cliaNum";
    private static final String CITY_PROPERTY = "city";
    private static final String STATE_PROPERTY = "state";
    private static final String TYPE_NAME_PROPERTY = "organizationTypes.name";
    private static final String PARENT_ID_PROPERTY = "organization.id";
    private static final String PARENT_UUID_PROPERTY = "organization.fhirUuid";

    public OrganizationSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Organization> search(OrganizationSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        FhirCriteriaContext<Organization, Organization> context = createCriteriaContext(Organization.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(OrganizationSearchParams params) {

        return count(Organization.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /**
     * Child organizations of the given parents (for
     * {@code _revinclude=Organization:partof}).
     */
    public List<Organization> findByParentIds(List<String> parentIds) {

        List<String> ids = parentIds == null ? List.of()
                : parentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        FhirCriteriaContext<Organization, Organization> context = createCriteriaContext(Organization.class);
        Expression<String> parentId = resolveExpression(context, PARENT_ID_PROPERTY, String.class);
        context.addPredicate(parentId.in(ids));
        context.distinct(true);
        return list(context);
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Organization, R> context,
            OrganizationSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createOrganizationIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createStringPredicate(context, NAME_PROPERTY, params.getName()));
        addPredicate(context, createActivePredicate(context, params.getActive()));
        addPredicate(context, createTypePredicate(context, params.getType()));
        addPredicate(context, createPartOfPredicate(context, params.getPartOf()));
        addPredicate(context, createStringPredicate(context, CITY_PROPERTY, params.getCity()));
        addPredicate(context, createStringPredicate(context, STATE_PROPERTY, params.getState()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    private <R> Optional<Predicate> createActivePredicate(FhirCriteriaContext<Organization, R> context,
            TokenAndListParam active) {

        if (active == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, ACTIVE_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, active, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            return switch (value) {
            case "true" -> Optional.of(criteriaBuilder.equal(criteriaBuilder.upper(expression), "Y"));
            case "false" -> Optional.of(criteriaBuilder.or(criteriaBuilder.isNull(expression),
                    criteriaBuilder.notEqual(criteriaBuilder.upper(expression), "Y")));
            default -> Optional.of(criteriaBuilder.disjunction());
            };
        });
    }

    /**
     * {@code type=<code>} matches the organization type name, case-insensitively.
     */
    private <R> Optional<Predicate> createTypePredicate(FhirCriteriaContext<Organization, R> context,
            TokenAndListParam type) {

        if (type == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, TYPE_NAME_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, type, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            return Optional
                    .of(criteriaBuilder.equal(criteriaBuilder.lower(expression), value.toLowerCase(Locale.ROOT)));
        });
    }

    /**
     * {@code partof=Organization/<id>} accepts the parent's FHIR uuid or OpenELIS
     * id.
     */
    private <R> Optional<Predicate> createPartOfPredicate(FhirCriteriaContext<Organization, R> context,
            ReferenceAndListParam partOf) {

        if (partOf == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<UUID> parentUuid = resolveUuidExpression(context, PARENT_UUID_PROPERTY);
        Expression<String> parentId = resolveStringExpression(context, PARENT_ID_PROPERTY);

        return handleAndListParam(criteriaBuilder, partOf, (ReferenceParam reference) -> {
            String idPart = reference.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(criteriaBuilder.equal(parentUuid, UUID.fromString(idPart.trim())));
            } catch (IllegalArgumentException e) {
                return Optional.of(criteriaBuilder.equal(parentId, idPart.trim()));
            }
        });
    }

    private <R> Optional<Predicate> createOrganizationIdentifierPredicate(FhirCriteriaContext<Organization, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, identifier, token -> identifierPredicate(context, token));
    }

    private <R> Optional<Predicate> identifierPredicate(FhirCriteriaContext<Organization, R> context,
            TokenParam token) {

        String value = token.getValue() == null ? "" : token.getValue().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Root<Organization> root = context.getRoot();
        String system = normalizeSystem(token.getSystem());
        String systemKey = system.contains("/") ? system.substring(system.lastIndexOf('/') + 1) : system;

        List<Predicate> alternatives = new ArrayList<>();
        switch (systemKey) {
        case "":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            alternatives.add(criteriaBuilder.equal(root.get(CODE_PROPERTY), value));
            alternatives.add(criteriaBuilder.equal(root.get(SHORT_NAME_PROPERTY), value));
            alternatives.add(criteriaBuilder.equal(root.get(CLIA_PROPERTY), value));
            break;
        case "org_uuid":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            break;
        case "org_code":
            alternatives.add(criteriaBuilder.equal(root.get(CODE_PROPERTY), value));
            break;
        case "org_shortName":
            alternatives.add(criteriaBuilder.equal(root.get(SHORT_NAME_PROPERTY), value));
            break;
        case "org_cliaNum":
            alternatives.add(criteriaBuilder.equal(root.get(CLIA_PROPERTY), value));
            break;
        default:
            break;
        }
        if (alternatives.isEmpty()) {
            return Optional.of(criteriaBuilder.disjunction());
        }
        return combineWithOr(criteriaBuilder, alternatives);
    }

    private Optional<Predicate> uuidPredicate(CriteriaBuilder criteriaBuilder, Root<Organization> root, String value) {
        try {
            return Optional
                    .of(criteriaBuilder.equal(root.<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
