package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.DateRangeParam;
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
import org.openelisglobal.fhir.search.searchparams.SpecimenSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS SampleItem records with FHIR Specimen search parameters.
 *
 * <p>
 * Identifier systems mirror the transform: {@code sampleItem_uuid} and
 * {@code sampleItem_labNo} (accession number, optionally suffixed with the item
 * sort order). Status codes are the inverse of the outbound mapping: available
 * excludes cancelled and disposed items, unsatisfactory is cancelled,
 * unavailable is disposed.
 */
@Repository
@Transactional(readOnly = true)
public class SpecimenSearchDao extends BaseFhirDao {

    private static final String SAMPLE_ID_PROPERTY = "sample.id";
    private static final String ACCESSION_PROPERTY = "sample.accessionNumber";
    private static final String SORT_ORDER_PROPERTY = "sortOrder";
    private static final String EXTERNAL_ID_PROPERTY = "externalId";
    private static final String STATUS_PROPERTY = "statusId";
    private static final String COLLECTION_DATE_PROPERTY = "collectionDate";
    private static final String TYPE_ABBREVIATION_PROPERTY = "typeOfSample.localAbbreviation";
    private static final String TYPE_DESCRIPTION_PROPERTY = "typeOfSample.description";

    public SpecimenSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<SampleItem> search(SpecimenSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        FhirCriteriaContext<SampleItem, SampleItem> context = createCriteriaContext(SampleItem.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(SpecimenSearchParams params) {

        return count(SampleItem.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    /** Every sample item belonging to one of the given Sample primary keys. */
    public List<SampleItem> findBySampleIds(List<String> sampleIds) {

        List<String> ids = distinctIds(sampleIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        FhirCriteriaContext<SampleItem, SampleItem> context = createCriteriaContext(SampleItem.class);
        Expression<String> sampleId = resolveExpression(context, SAMPLE_ID_PROPERTY, String.class);
        context.addPredicate(sampleId.in(ids));
        context.distinct(true);
        return list(context);
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<SampleItem, R> context, SpecimenSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createSpecimenIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createAccessionPredicate(context, params.getAccession()));
        addPredicate(context, createPatientPredicate(context, params.getPatient()));
        addPredicate(context, createTypePredicate(context, params.getType()));
        addPredicate(context, createStatusPredicate(context, params));
        addPredicate(context, createCollectedPredicate(context, params.getCollected()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /**
     * Matches the accession as the resource publishes it.
     *
     * <p>
     * {@code accessionIdentifier} carries {@code accession[-sortOrder]}, so a
     * client searching with the value it was just given has to get a hit; a bare
     * accession still matches every item of the sample.
     */
    private <R> Optional<Predicate> createAccessionPredicate(FhirCriteriaContext<SampleItem, R> context,
            TokenAndListParam accession) {

        if (accession == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, accession, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim();
            if (value.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(labNumberPredicate(context, value));
        });
    }

    /**
     * {@code patient=Patient/<uuid>} resolves through sample_human; a non-uuid
     * value is taken as the OpenELIS patient id.
     */
    private <R> Optional<Predicate> createPatientPredicate(FhirCriteriaContext<SampleItem, R> context,
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

    private <R> Subquery<String> sampleIdsForPatient(FhirCriteriaContext<SampleItem, R> context, String patientRef) {

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

    /** {@code type=<code>} matches the sample type abbreviation or description. */
    private <R> Optional<Predicate> createTypePredicate(FhirCriteriaContext<SampleItem, R> context,
            TokenAndListParam type) {

        if (type == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> abbreviation = resolveStringExpression(context, TYPE_ABBREVIATION_PROPERTY);
        Expression<String> description = resolveStringExpression(context, TYPE_DESCRIPTION_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, type, token -> {
            String value = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(criteriaBuilder.or(criteriaBuilder.equal(criteriaBuilder.lower(abbreviation), value),
                    criteriaBuilder.equal(criteriaBuilder.lower(description), value)));
        });
    }

    private <R> Optional<Predicate> createStatusPredicate(FhirCriteriaContext<SampleItem, R> context,
            SpecimenSearchParams params) {

        TokenAndListParam status = params.getStatus();
        if (status == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, STATUS_PROPERTY);
        List<String> terminal = new ArrayList<>();
        if (params.getCanceledStatusId() != null) {
            terminal.add(params.getCanceledStatusId());
        }
        if (params.getDisposedStatusId() != null) {
            terminal.add(params.getDisposedStatusId());
        }

        return handleTokenAndListParam(criteriaBuilder, status, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            return switch (code) {
            case "available" -> Optional.of(terminal.isEmpty() ? criteriaBuilder.conjunction()
                    : criteriaBuilder.or(criteriaBuilder.isNull(expression),
                            criteriaBuilder.not(expression.in(terminal))));
            case "unsatisfactory" -> Optional.of(params.getCanceledStatusId() == null ? criteriaBuilder.disjunction()
                    : criteriaBuilder.equal(expression, params.getCanceledStatusId()));
            case "unavailable" -> Optional.of(params.getDisposedStatusId() == null ? criteriaBuilder.disjunction()
                    : criteriaBuilder.equal(expression, params.getDisposedStatusId()));
            default -> Optional.of(criteriaBuilder.disjunction());
            };
        });
    }

    private <R> Optional<Predicate> createCollectedPredicate(FhirCriteriaContext<SampleItem, R> context,
            DateRangeParam collected) {

        if (collected == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<Date> expression = resolveExpression(context, COLLECTION_DATE_PROPERTY, Date.class);
        return combineWithAnd(criteriaBuilder, DateParamBounds.predicates(criteriaBuilder, expression, collected));
    }

    private <R> Optional<Predicate> createSpecimenIdentifierPredicate(FhirCriteriaContext<SampleItem, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, identifier, token -> identifierPredicate(context, token));
    }

    private <R> Optional<Predicate> identifierPredicate(FhirCriteriaContext<SampleItem, R> context, TokenParam token) {

        String value = token.getValue() == null ? "" : token.getValue().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        String system = normalizeSystem(token.getSystem());
        String systemKey = system.contains("/") ? system.substring(system.lastIndexOf('/') + 1) : system;

        List<Predicate> alternatives = new ArrayList<>();
        switch (systemKey) {
        case "":
            Optional<Predicate> uuid = uuidPredicate(context, value);
            if (uuid.isPresent()) {
                alternatives.add(uuid.get());
            } else {
                alternatives.add(labNumberPredicate(context, value));
                alternatives.add(criteriaBuilder.equal(context.getRoot().get(EXTERNAL_ID_PROPERTY), value));
            }
            break;
        case "sampleItem_uuid":
            uuidPredicate(context, value).ifPresent(alternatives::add);
            break;
        case "sampleItem_labNo":
            alternatives.add(labNumberPredicate(context, value));
            break;
        default:
            break;
        }
        if (alternatives.isEmpty()) {
            return Optional.of(criteriaBuilder.disjunction());
        }
        return combineWithOr(criteriaBuilder, alternatives);
    }

    /**
     * The lab number identifier is {@code accession[-sortOrder]}; the plain
     * accession matches every item of the sample.
     */
    private <R> Predicate labNumberPredicate(FhirCriteriaContext<SampleItem, R> context, String value) {

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> accession = resolveStringExpression(context, ACCESSION_PROPERTY);
        Expression<String> sortOrder = resolveStringExpression(context, SORT_ORDER_PROPERTY);

        int dash = value.lastIndexOf('-');
        Predicate whole = criteriaBuilder.equal(accession, value);
        if (dash <= 0 || dash == value.length() - 1
                || !value.substring(dash + 1).chars().allMatch(Character::isDigit)) {
            return whole;
        }
        return criteriaBuilder.or(whole, criteriaBuilder.and(criteriaBuilder.equal(accession, value.substring(0, dash)),
                criteriaBuilder.equal(sortOrder, value.substring(dash + 1))));
    }

    private <R> Optional<Predicate> uuidPredicate(FhirCriteriaContext<SampleItem, R> context, String value) {
        try {
            return Optional.of(requireCriteriaBuilder(context)
                    .equal(context.getRoot().<UUID>get(FhirConstants.ID_PROPERTY), UUID.fromString(value)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static List<String> distinctIds(List<String> ids) {
        return ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).map(String::trim).filter(id -> !id.isEmpty()).distinct()
                        .toList();
    }
}
