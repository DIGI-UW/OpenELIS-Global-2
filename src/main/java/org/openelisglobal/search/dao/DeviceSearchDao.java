package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.DeviceSearchParams;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Analyzer records with FHIR Device search parameters.
 * Identifier systems mirror the transform: {@code analyzer_uuid},
 * {@code analyzer_machineId} and {@code analyzer_sourceId}; status codes are
 * the inverse of the outbound analyzer-status mapping.
 */
@Repository
@Transactional(readOnly = true)
public class DeviceSearchDao extends BaseFhirDao {

    private static final String NAME_PROPERTY = "name";
    private static final String TYPE_PROPERTY = "type";
    private static final String STATUS_PROPERTY = "status";
    private static final String MACHINE_ID_PROPERTY = "machineId";
    private static final String SOURCE_ID_PROPERTY = "discoveredSourceId";

    public DeviceSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Analyzer> search(DeviceSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        FhirCriteriaContext<Analyzer, Analyzer> context = createCriteriaContext(Analyzer.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(DeviceSearchParams params) {

        return count(Analyzer.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Analyzer, R> context, DeviceSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createDeviceIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createStringPredicate(context, NAME_PROPERTY, params.getDeviceName()));
        addPredicate(context, createTypePredicate(context, params.getType()));
        addPredicate(context, createStatusPredicate(context, params.getStatus()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    private <R> Optional<Predicate> createTypePredicate(FhirCriteriaContext<Analyzer, R> context,
            TokenAndListParam type) {

        if (type == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, TYPE_PROPERTY);

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
     * FHIR device status back to analyzer statuses: active covers setup,
     * validation, active and offline analyzers; inactive covers inactive and
     * deleted; entered-in-error is error-pending; unknown is pending registration.
     */
    private <R> Optional<Predicate> createStatusPredicate(FhirCriteriaContext<Analyzer, R> context,
            TokenAndListParam status) {

        if (status == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<AnalyzerStatus> expression = resolveExpression(context, STATUS_PROPERTY, AnalyzerStatus.class);

        return handleTokenAndListParam(criteriaBuilder, status, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            return switch (code) {
            case "active" ->
                Optional.of(criteriaBuilder.or(criteriaBuilder.isNull(expression), expression.in(AnalyzerStatus.SETUP,
                        AnalyzerStatus.VALIDATION, AnalyzerStatus.ACTIVE, AnalyzerStatus.OFFLINE)));
            case "inactive" -> Optional.of(expression.in(AnalyzerStatus.INACTIVE, AnalyzerStatus.DELETED));
            case "entered-in-error" -> Optional.of(criteriaBuilder.equal(expression, AnalyzerStatus.ERROR_PENDING));
            case "unknown" -> Optional.of(criteriaBuilder.equal(expression, AnalyzerStatus.PENDING_REGISTRATION));
            default -> Optional.of(criteriaBuilder.disjunction());
            };
        });
    }

    private <R> Optional<Predicate> createDeviceIdentifierPredicate(FhirCriteriaContext<Analyzer, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, identifier, token -> identifierPredicate(context, token));
    }

    private <R> Optional<Predicate> identifierPredicate(FhirCriteriaContext<Analyzer, R> context, TokenParam token) {

        String value = token.getValue() == null ? "" : token.getValue().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Root<Analyzer> root = context.getRoot();
        String system = normalizeSystem(token.getSystem());
        String systemKey = system.contains("/") ? system.substring(system.lastIndexOf('/') + 1) : system;

        List<Predicate> alternatives = new ArrayList<>();
        switch (systemKey) {
        case "":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            alternatives.add(criteriaBuilder.equal(root.get(MACHINE_ID_PROPERTY), value));
            alternatives.add(criteriaBuilder.equal(root.get(SOURCE_ID_PROPERTY), value));
            break;
        case "analyzer_uuid":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            break;
        case "analyzer_machineId":
            alternatives.add(criteriaBuilder.equal(root.get(MACHINE_ID_PROPERTY), value));
            break;
        case "analyzer_sourceId":
            alternatives.add(criteriaBuilder.equal(root.get(SOURCE_ID_PROPERTY), value));
            break;
        default:
            break;
        }
        if (alternatives.isEmpty()) {
            return Optional.of(criteriaBuilder.disjunction());
        }
        return combineWithOr(criteriaBuilder, alternatives);
    }

    private Optional<Predicate> uuidPredicate(CriteriaBuilder criteriaBuilder, Root<Analyzer> root, String value) {
        try {
            return Optional
                    .of(criteriaBuilder.equal(root.<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
