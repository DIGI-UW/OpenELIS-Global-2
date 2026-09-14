package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.dao.DateParamBounds;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ObservationSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Result records with FHIR Observation search parameters. The
 * order is the analysis ({@code based-on}), the specimen is the analysis'
 * sample item, and the effective date is the analysis release date.
 */
@Repository
@Transactional(readOnly = true)
public class ObservationSearchDao extends BaseFhirDao {

    private static final String ANALYSIS_ID_PROPERTY = "analysis.id";
    private static final String ANALYSIS_UUID_PROPERTY = "analysis." + FhirConstants.ID_PROPERTY;
    private static final String ANALYSIS_STATUS_PROPERTY = "analysis.statusId";
    private static final String RELEASED_DATE_PROPERTY = "analysis.releasedDate";
    private static final String SAMPLE_ID_PROPERTY = "analysis." + FhirConstants.ANALYSIS_SAMPLE_ID_HANDLER;
    private static final String SAMPLE_ITEM_UUID_PROPERTY = "analysis." + FhirConstants.SAMPLE_ITEM_FHIR_UUID_HANDLER;
    private static final String SAMPLE_ITEM_ID_PROPERTY = "analysis." + FhirConstants.SAMPLE_ITEM_ID_HANDLER;
    private static final String TEST_LOINC_PROPERTY = "analysis.test.loinc";
    private static final String TEST_NAME_PROPERTY = "analysis.test.description";

    public ObservationSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Result> search(ObservationSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        FhirCriteriaContext<Result, Result> context = createCriteriaContext(Result.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(ObservationSearchParams params) {

        return count(Result.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /** Every result recorded against one of the given Analysis primary keys. */
    public List<Result> findByAnalysisIds(List<String> analysisIds) {

        List<String> ids = analysisIds == null ? List.of()
                : analysisIds.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).distinct()
                        .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        FhirCriteriaContext<Result, Result> context = createCriteriaContext(Result.class);
        Expression<String> analysisId = resolveExpression(context, ANALYSIS_ID_PROPERTY, String.class);
        context.addPredicate(analysisId.in(ids));
        context.distinct(true);
        return list(context);
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Result, R> context, ObservationSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createPatientPredicate(context, params.getPatient()));
        addPredicate(context, createUuidReferencePredicate(context, params.getBasedOn(), ANALYSIS_UUID_PROPERTY,
                ANALYSIS_ID_PROPERTY));
        addPredicate(context, createUuidReferencePredicate(context, params.getSpecimen(), SAMPLE_ITEM_UUID_PROPERTY,
                SAMPLE_ITEM_ID_PROPERTY));
        addPredicate(context, createCodePredicate(context, params.getCode()));
        addPredicate(context, createStatusPredicate(context, params));
        addPredicate(context, createDatePredicate(context, params.getDate()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    private <R> Optional<Predicate> createPatientPredicate(FhirCriteriaContext<Result, R> context,
            ReferenceAndListParam patient) {

        if (patient == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> sampleId = resolveStringExpression(context, SAMPLE_ID_PROPERTY);

        return handleAndListParam(criteriaBuilder, patient, (ReferenceParam reference) -> {
            String idPart = reference.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(sampleId.in(sampleIdsForPatient(context, idPart.trim())));
        });
    }

    private <R> Subquery<String> sampleIdsForPatient(FhirCriteriaContext<Result, R> context, String patientRef) {

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        FhirCriteriaContext<SampleHuman, String> sub = createSubQuery(context, SampleHuman.class, String.class);
        Subquery<String> subquery = sub.getSubquery();
        Root<SampleHuman> sampleHuman = sub.getRoot();
        Root<Patient> patient = subquery.from(Patient.class);
        subquery.select(sampleHuman.get(FhirConstants.SAMPLE_ID));

        List<Predicate> where = new ArrayList<>();
        where.add(criteriaBuilder.equal(sampleHuman.get(FhirConstants.PATIENT_ID), patient.get("id")));
        try {
            where.add(criteriaBuilder.equal(patient.<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(patientRef)));
        } catch (IllegalArgumentException e) {
            where.add(criteriaBuilder.equal(patient.get("id"), patientRef));
        }
        subquery.where(where.toArray(Predicate[]::new));
        return subquery;
    }

    /**
     * A reference whose id is the linked row's FHIR uuid, or its OpenELIS id as a
     * fallback.
     */
    private <R> Optional<Predicate> createUuidReferencePredicate(FhirCriteriaContext<Result, R> context,
            ReferenceAndListParam reference, String uuidProperty, String idProperty) {

        if (reference == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<UUID> uuidExpression = resolveUuidExpression(context, uuidProperty);
        Expression<String> idExpression = resolveStringExpression(context, idProperty);

        return handleAndListParam(criteriaBuilder, reference, (ReferenceParam value) -> {
            String idPart = value.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(criteriaBuilder.equal(uuidExpression, UUID.fromString(idPart.trim())));
            } catch (IllegalArgumentException e) {
                return Optional.of(criteriaBuilder.equal(idExpression, idPart.trim()));
            }
        });
    }

    private <R> Optional<Predicate> createCodePredicate(FhirCriteriaContext<Result, R> context,
            TokenAndListParam code) {

        if (code == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> loinc = resolveStringExpression(context, TEST_LOINC_PROPERTY);
        Expression<String> name = resolveStringExpression(context, TEST_NAME_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, code, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            String system = normalizeSystem(token.getSystem());
            if (system.isEmpty()) {
                return Optional.of(criteriaBuilder.or(criteriaBuilder.equal(loinc, value),
                        criteriaBuilder.equal(criteriaBuilder.lower(name), value.toLowerCase(Locale.ROOT))));
            }
            if ("http://loinc.org".equalsIgnoreCase(system)) {
                return Optional.of(criteriaBuilder.equal(loinc, value));
            }
            return Optional.of(criteriaBuilder.disjunction());
        });
    }

    private <R> Optional<Predicate> createStatusPredicate(FhirCriteriaContext<Result, R> context,
            ObservationSearchParams params) {

        TokenAndListParam status = params.getStatus();
        if (status == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, ANALYSIS_STATUS_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, status, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            List<String> ids = params.getStatusIdsByCode().getOrDefault(code, List.of());
            if ("preliminary".equals(code)) {
                List<String> others = new ArrayList<>();
                params.getStatusIdsByCode().forEach((key, value) -> {
                    if (!"preliminary".equals(key)) {
                        others.addAll(value);
                    }
                });
                return Optional.of(
                        others.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.not(expression.in(others)));
            }
            if (ids.isEmpty()) {
                return Optional.of(criteriaBuilder.disjunction());
            }
            return Optional.of(expression.in(ids));
        });
    }

    private <R> Optional<Predicate> createDatePredicate(FhirCriteriaContext<Result, R> context, DateRangeParam date) {

        if (date == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<Date> expression = resolveExpression(context, RELEASED_DATE_PROPERTY, Date.class);
        return combineWithAnd(criteriaBuilder, DateParamBounds.predicates(criteriaBuilder, expression, date));
    }
}
