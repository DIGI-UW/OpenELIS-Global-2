package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Analysis records with FHIR ServiceRequest search
 * parameters. Identifier systems mirror the transform: {@code analysis_uuid}
 * (the logical id) and {@code samp_labNo} (the requisition, i.e. the sample
 * accession number).
 */
@Repository
@Transactional(readOnly = true)
public class ServiceRequestSearchDao extends BaseFhirDao {

    private static final String SAMPLE_ID_PROPERTY = FhirConstants.ANALYSIS_SAMPLE_ID_HANDLER;
    private static final String ACCESSION_PROPERTY = FhirConstants.SAMPLE_ITEM + "." + FhirConstants.SAMPLE
            + ".accessionNumber";
    private static final String SAMPLE_ITEM_UUID_PROPERTY = FhirConstants.SAMPLE_ITEM_FHIR_UUID_HANDLER;
    private static final String TEST_LOINC_PROPERTY = "test.loinc";
    private static final String TEST_NAME_PROPERTY = "test.description";
    private static final String TEST_SHORT_NAME_PROPERTY = "test.name";
    private static final String STATUS_PROPERTY = "statusId";

    public ServiceRequestSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Analysis> search(ServiceRequestSearchParams params) {

        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context);
    }

    public List<Analysis> search(ServiceRequestSearchParams params, int offset, int pageSize) {

        validatePagination(offset, pageSize);
        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(ServiceRequestSearchParams params) {

        return count(Analysis.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /**
     * Analyses whose sample belongs to one of the given sample_human rows (the
     * Practitioner and Patient reverse-include path).
     */
    public List<Analysis> findBySampleHumans(List<SampleHuman> sampleHumans) {

        List<String> sampleIds = sampleHumans == null ? List.of()
                : sampleHumans.stream().filter(Objects::nonNull).map(SampleHuman::getSampleId).filter(Objects::nonNull)
                        .map(String::trim).filter(id -> !id.isEmpty()).distinct().toList();
        if (sampleIds.isEmpty()) {
            return List.of();
        }
        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);
        Expression<String> analysisSampleId = resolveExpression(context, SAMPLE_ID_PROPERTY, String.class);
        context.addPredicate(analysisSampleId.in(sampleIds));
        context.distinct(true);
        return list(context);
    }

    /** Analyses recorded against the given SampleItem primary keys. */
    public List<Analysis> findBySampleItemIds(List<String> sampleItemIds) {

        List<String> ids = sampleItemIds == null ? List.of()
                : sampleItemIds.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty())
                        .distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        FhirCriteriaContext<Analysis, Analysis> context = createCriteriaContext(Analysis.class);
        Expression<String> sampleItemId = resolveExpression(context, FhirConstants.SAMPLE_ITEM_ID_HANDLER,
                String.class);
        context.addPredicate(sampleItemId.in(ids));
        context.distinct(true);
        return list(context);
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Analysis, R> context, ServiceRequestSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createServiceRequestIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createPatientPredicate(context, params.getPatient()));
        addPredicate(context, createRequesterPredicate(context, params.getRequester()));
        addPredicate(context, createSpecimenPredicate(context, params.getSpecimen()));
        addPredicate(context, createCodePredicate(context, params.getCode()));
        addPredicate(context, createStatusPredicate(context, params));
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
            return Optional
                    .of(sampleId.in(sampleIdsFor(context, Patient.class, FhirConstants.PATIENT_ID, idPart.trim())));
        });
    }

    private <R> Optional<Predicate> createRequesterPredicate(FhirCriteriaContext<Analysis, R> context,
            ReferenceAndListParam requester) {

        if (requester == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> sampleId = resolveStringExpression(context, SAMPLE_ID_PROPERTY);

        return handleAndListParam(criteriaBuilder, requester, (ReferenceParam reference) -> {
            String idPart = reference.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            return Optional
                    .of(sampleId.in(sampleIdsFor(context, Provider.class, FhirConstants.PROVIDER_ID, idPart.trim())));
        });
    }

    /**
     * {@code select sh.sampleId from SampleHuman sh, <Patient|Provider> x where
     * sh.<column> = x.id and (x.fhirUuid = :uuid or x.id = :id)}.
     */
    private <R, X> Subquery<String> sampleIdsFor(FhirCriteriaContext<Analysis, R> context, Class<X> linkedType,
            String sampleHumanColumn, String reference) {

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        FhirCriteriaContext<SampleHuman, String> sub = createSubQuery(context, SampleHuman.class, String.class);
        Subquery<String> subquery = sub.getSubquery();
        Root<SampleHuman> sampleHuman = sub.getRoot();
        Root<X> linked = subquery.from(linkedType);
        subquery.select(sampleHuman.get(FhirConstants.SAMPLE_ID));

        List<Predicate> where = new ArrayList<>();
        where.add(criteriaBuilder.equal(sampleHuman.get(sampleHumanColumn), linked.get("id")));
        try {
            where.add(criteriaBuilder.equal(linked.<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(reference)));
        } catch (IllegalArgumentException e) {
            where.add(criteriaBuilder.equal(linked.get("id"), reference));
        }
        subquery.where(where.toArray(Predicate[]::new));
        return subquery;
    }

    private <R> Optional<Predicate> createSpecimenPredicate(FhirCriteriaContext<Analysis, R> context,
            ReferenceAndListParam specimen) {

        if (specimen == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<UUID> sampleItemUuid = resolveUuidExpression(context, SAMPLE_ITEM_UUID_PROPERTY);
        Expression<String> sampleItemId = resolveStringExpression(context, FhirConstants.SAMPLE_ITEM_ID_HANDLER);

        return handleAndListParam(criteriaBuilder, specimen, (ReferenceParam reference) -> {
            String idPart = reference.getIdPart();
            if (idPart == null || idPart.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(criteriaBuilder.equal(sampleItemUuid, UUID.fromString(idPart.trim())));
            } catch (IllegalArgumentException e) {
                return Optional.of(criteriaBuilder.equal(sampleItemId, idPart.trim()));
            }
        });
    }

    /**
     * {@code code=http://loinc.org|<code>} matches the test's LOINC; a bare value
     * matches the LOINC or the test name.
     */
    private <R> Optional<Predicate> createCodePredicate(FhirCriteriaContext<Analysis, R> context,
            TokenAndListParam code) {

        if (code == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> loinc = resolveStringExpression(context, TEST_LOINC_PROPERTY);
        Expression<String> name = resolveStringExpression(context, TEST_NAME_PROPERTY);
        Expression<String> shortName = resolveStringExpression(context, TEST_SHORT_NAME_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, code, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            String system = normalizeSystem(token.getSystem());
            if (system.isEmpty()) {
                String lowered = value.toLowerCase(Locale.ROOT);
                return Optional.of(criteriaBuilder.or(criteriaBuilder.equal(loinc, value),
                        criteriaBuilder.equal(criteriaBuilder.lower(name), lowered),
                        criteriaBuilder.equal(criteriaBuilder.lower(shortName), lowered)));
            }
            if ("http://loinc.org".equalsIgnoreCase(system)) {
                return Optional.of(criteriaBuilder.equal(loinc, value));
            }
            return Optional.of(criteriaBuilder.disjunction());
        });
    }

    private <R> Optional<Predicate> createStatusPredicate(FhirCriteriaContext<Analysis, R> context,
            ServiceRequestSearchParams params) {

        TokenAndListParam status = params.getStatus();
        if (status == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, STATUS_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, status, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            List<String> ids = params.getStatusIdsByCode().getOrDefault(code, List.of());
            if (ids.isEmpty()) {
                return Optional.of(criteriaBuilder.disjunction());
            }
            return Optional.of(expression.in(ids));
        });
    }

    private <R> Optional<Predicate> createServiceRequestIdentifierPredicate(FhirCriteriaContext<Analysis, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, identifier, token -> identifierPredicate(context, token));
    }

    private <R> Optional<Predicate> identifierPredicate(FhirCriteriaContext<Analysis, R> context, TokenParam token) {

        String value = token.getValue() == null ? "" : token.getValue().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> accession = resolveStringExpression(context, ACCESSION_PROPERTY);
        String system = normalizeSystem(token.getSystem());
        String systemKey = system.contains("/") ? system.substring(system.lastIndexOf('/') + 1) : system;

        List<Predicate> alternatives = new ArrayList<>();
        switch (systemKey) {
        case "":
            uuidPredicate(context, value).ifPresent(alternatives::add);
            alternatives.add(criteriaBuilder.equal(accession, value));
            break;
        case "analysis_uuid":
            uuidPredicate(context, value).ifPresent(alternatives::add);
            break;
        case "samp_labNo":
            alternatives.add(criteriaBuilder.equal(accession, value));
            break;
        default:
            break;
        }
        if (alternatives.isEmpty()) {
            return Optional.of(criteriaBuilder.disjunction());
        }
        return combineWithOr(criteriaBuilder, alternatives);
    }

    private <R> Optional<Predicate> uuidPredicate(FhirCriteriaContext<Analysis, R> context, String value) {
        try {
            return Optional.of(requireCriteriaBuilder(context)
                    .equal(context.getRoot().<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void validatePagination(int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
    }
}
