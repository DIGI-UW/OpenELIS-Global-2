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
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.dao.DateParamBounds;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.DiagnosticReportSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Analysis records with FHIR DiagnosticReport search
 * parameters. A report shares its logical id with the ServiceRequest for the
 * same analysis, so {@code based-on} and {@code _id} resolve the same rows;
 * {@code result} resolves through the analysis' results; {@code issued} is the
 * analysis release date.
 */
@Repository
@Transactional(readOnly = true)
public class DiagnosticReportSearchDao extends BaseFhirDao {

    private static final String STATUS_PROPERTY = "statusId";
    private static final String RELEASED_DATE_PROPERTY = "releasedDate";
    private static final String SAMPLE_ID_PROPERTY = FhirConstants.ANALYSIS_SAMPLE_ID_HANDLER;
    private static final String SAMPLE_ITEM_UUID_PROPERTY = FhirConstants.SAMPLE_ITEM_FHIR_UUID_HANDLER;
    private static final String SAMPLE_ITEM_ID_PROPERTY = FhirConstants.SAMPLE_ITEM_ID_HANDLER;
    private static final String TEST_LOINC_PROPERTY = "test.loinc";
    private static final String TEST_NAME_PROPERTY = "test.description";

    public DiagnosticReportSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Analysis> search(DiagnosticReportSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(DiagnosticReportSearchParams params) {

        return count(Analysis.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Analysis, R> context,
            DiagnosticReportSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createPatientPredicate(context, params.getPatient()));
        addPredicate(context,
                createUuidReferencePredicate(context, params.getBasedOn(), FhirConstants.ID_PROPERTY, "id"));
        addPredicate(context, createResultPredicate(context, params.getResult()));
        addPredicate(context, createUuidReferencePredicate(context, params.getSpecimen(), SAMPLE_ITEM_UUID_PROPERTY,
                SAMPLE_ITEM_ID_PROPERTY));
        addPredicate(context, createCodePredicate(context, params.getCode()));
        addPredicate(context, createStatusPredicate(context, params));
        addPredicate(context, createIssuedPredicate(context, params.getIssued()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    private <R> Optional<Predicate> createPatientPredicate(FhirCriteriaContext<Analysis, R> context,
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

    private <R> Subquery<String> sampleIdsForPatient(FhirCriteriaContext<Analysis, R> context, String patientRef) {

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

    /** {@code result=Observation/<uuid>}: analyses owning a result with that id. */
    private <R> Optional<Predicate> createResultPredicate(FhirCriteriaContext<Analysis, R> context,
            ReferenceAndListParam result) {

        if (result == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        return handleAndListParam(criteriaBuilder, result, (ReferenceParam reference) -> {
            String idPart = reference.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            FhirCriteriaContext<Result, String> sub = createSubQuery(context, Result.class, String.class);
            Subquery<String> subquery = sub.getSubquery();
            Root<Result> resultRoot = sub.getRoot();
            subquery.select(resultRoot.get("analysis").get("id"));
            try {
                subquery.where(criteriaBuilder.equal(resultRoot.<UUID>get(FhirConstants.ID_PROPERTY),
                        UUID.fromString(idPart.trim())));
            } catch (IllegalArgumentException e) {
                subquery.where(criteriaBuilder.equal(resultRoot.get("id"), idPart.trim()));
            }
            return Optional.of(context.getRoot().get("id").in(subquery));
        });
    }

    private <R> Optional<Predicate> createUuidReferencePredicate(FhirCriteriaContext<Analysis, R> context,
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

    private <R> Optional<Predicate> createCodePredicate(FhirCriteriaContext<Analysis, R> context,
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

    private <R> Optional<Predicate> createStatusPredicate(FhirCriteriaContext<Analysis, R> context,
            DiagnosticReportSearchParams params) {

        TokenAndListParam status = params.getStatus();
        if (status == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, STATUS_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, status, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            List<String> ids = params.getStatusIdsByCode().getOrDefault(code, List.of());
            if ("unknown".equals(code)) {
                List<String> mapped = new ArrayList<>();
                params.getStatusIdsByCode().values().forEach(mapped::addAll);
                return Optional.of(
                        mapped.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.not(expression.in(mapped)));
            }
            if (ids.isEmpty()) {
                return Optional.of(criteriaBuilder.disjunction());
            }
            return Optional.of(expression.in(ids));
        });
    }

    private <R> Optional<Predicate> createIssuedPredicate(FhirCriteriaContext<Analysis, R> context,
            DateRangeParam issued) {

        if (issued == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<Date> expression = resolveExpression(context, RELEASED_DATE_PROPERTY, Date.class);
        return combineWithAnd(criteriaBuilder, DateParamBounds.predicates(criteriaBuilder, expression, issued));
    }
}
