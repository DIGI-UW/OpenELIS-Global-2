package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.dao.DateParamBounds;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PatientSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searches OpenELIS Patient records with FHIR Patient search parameters.
 *
 * <p>
 * Identifiers map onto the same systems the Patient transform emits:
 * {@code pat_uuid} (fhirUuid), {@code pat_nationalId} (nationalId column or the
 * NATIONAL identity), {@code pat_subjectNumber}, {@code pat_stNumber} and
 * {@code pat_guid} (patient_identity rows). A bare value is matched against all
 * of them.
 */
@Repository
@Transactional(readOnly = true)
public class PatientSearchDao extends BaseFhirDao {

    private static final String BIRTH_DATE_PROPERTY = "birthDate";
    private static final String GENDER_PROPERTY = "gender";
    private static final String NATIONAL_ID_PROPERTY = "nationalId";
    private static final String EXTERNAL_ID_PROPERTY = "externalId";
    private static final String SUBJECT_IDENTITY = "SUBJECT";
    private static final String NATIONAL_IDENTITY = "NATIONAL";
    private static final String ST_IDENTITY = "ST";
    private static final String GUID_IDENTITY = "GUID";

    public PatientSearchDao(FhirPropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    public List<Patient> search(PatientSearchParams params, int offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be zero or greater");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        FhirCriteriaContext<Patient, Patient> context = createCriteriaContext(Patient.class);
        if (params != null) {
            addSearchPredicates(context, params);
        }
        context.distinct(true);
        return list(context, offset, pageSize);
    }

    public long count(PatientSearchParams params) {

        return count(Patient.class, context -> {
            if (params != null) {
                addSearchPredicates(context, params);
            }
        });
    }

    private <R> void addSearchPredicates(FhirCriteriaContext<Patient, R> context, PatientSearchParams params) {

        addPredicate(context, createIdPredicate(context, params.getId()));
        addPredicate(context, createPatientIdentifierPredicate(context, params.getIdentifier()));
        addPredicate(context, createNamePredicate(context, params.getName()));
        addPredicate(context,
                createStringPredicate(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER, params.getGiven()));
        addPredicate(context,
                createStringPredicate(context, FhirConstants.LAST_NAME_SEARCH_HANDLER, params.getFamily()));
        addPredicate(context, createBirthDatePredicate(context, params.getBirthDate()));
        addPredicate(context, createGenderPredicate(context, params.getGender()));
        addPredicate(context, createLastUpdatedPredicate(context, params.getLastUpdated()));
    }

    /** {@code Patient?name=x} matches either the given or the family name. */
    private <R> Optional<Predicate> createNamePredicate(FhirCriteriaContext<Patient, R> context,
            StringAndListParam name) {

        if (name == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> given = resolveStringExpression(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER);
        Expression<String> family = resolveStringExpression(context, FhirConstants.LAST_NAME_SEARCH_HANDLER);

        return handleStringAndListParam(criteriaBuilder, name,
                parameter -> combineWithOr(criteriaBuilder,
                        Stream.of(createSingleStringPredicate(criteriaBuilder, given, parameter),
                                createSingleStringPredicate(criteriaBuilder, family, parameter))
                                .flatMap(Optional::stream).toList()));
    }

    /**
     * Day-precision values span the calendar day in the server zone, and the
     * {@code gt}/{@code lt} prefixes stay exclusive.
     */
    private <R> Optional<Predicate> createBirthDatePredicate(FhirCriteriaContext<Patient, R> context,
            DateRangeParam birthDate) {

        if (birthDate == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<Date> expression = resolveExpression(context, BIRTH_DATE_PROPERTY, Date.class);
        return combineWithAnd(criteriaBuilder, DateParamBounds.predicates(criteriaBuilder, expression, birthDate));
    }

    /**
     * FHIR administrative gender codes against the single-letter OpenELIS column:
     * male/female map to M/F, unknown to a missing value, other to anything else.
     */
    private <R> Optional<Predicate> createGenderPredicate(FhirCriteriaContext<Patient, R> context,
            TokenAndListParam gender) {

        if (gender == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Expression<String> expression = resolveStringExpression(context, GENDER_PROPERTY);

        return handleTokenAndListParam(criteriaBuilder, gender, token -> {
            String code = token.getValue() == null ? "" : token.getValue().trim().toLowerCase(Locale.ROOT);
            return switch (code) {
            case "male" -> Optional.of(criteriaBuilder.equal(criteriaBuilder.upper(expression), "M"));
            case "female" -> Optional.of(criteriaBuilder.equal(criteriaBuilder.upper(expression), "F"));
            case "unknown" -> Optional.of(criteriaBuilder.or(criteriaBuilder.isNull(expression),
                    criteriaBuilder.equal(criteriaBuilder.trim(expression), "")));
            case "other" -> Optional.of(criteriaBuilder.and(criteriaBuilder.isNotNull(expression),
                    criteriaBuilder.not(criteriaBuilder.upper(expression).in("M", "F"))));
            default -> Optional.of(criteriaBuilder.disjunction());
            };
        });
    }

    private <R> Optional<Predicate> createPatientIdentifierPredicate(FhirCriteriaContext<Patient, R> context,
            TokenAndListParam identifier) {

        if (identifier == null) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        return handleTokenAndListParam(criteriaBuilder, identifier, token -> identifierPredicate(context, token));
    }

    private <R> Optional<Predicate> identifierPredicate(FhirCriteriaContext<Patient, R> context, TokenParam token) {

        String value = token.getValue() == null ? "" : token.getValue().trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        Root<Patient> root = context.getRoot();
        String system = normalizeSystem(token.getSystem());
        String systemKey = system.contains("/") ? system.substring(system.lastIndexOf('/') + 1) : system;

        List<Predicate> alternatives = new ArrayList<>();
        switch (systemKey) {
        case "":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            alternatives.add(criteriaBuilder.equal(root.get(NATIONAL_ID_PROPERTY), value));
            alternatives.add(criteriaBuilder.equal(root.get(EXTERNAL_ID_PROPERTY), value));
            alternatives.add(identityPredicate(context, value, null));
            break;
        case "pat_uuid":
            uuidPredicate(criteriaBuilder, root, value).ifPresent(alternatives::add);
            break;
        case "pat_nationalId":
            alternatives.add(criteriaBuilder.equal(root.get(NATIONAL_ID_PROPERTY), value));
            alternatives.add(identityPredicate(context, value, NATIONAL_IDENTITY));
            break;
        case "pat_subjectNumber":
            alternatives.add(identityPredicate(context, value, SUBJECT_IDENTITY));
            break;
        case "pat_stNumber":
            alternatives.add(identityPredicate(context, value, ST_IDENTITY));
            break;
        case "pat_guid":
            alternatives.add(identityPredicate(context, value, GUID_IDENTITY));
            break;
        default:
            break;
        }
        if (alternatives.isEmpty()) {
            return Optional.of(criteriaBuilder.disjunction());
        }
        return combineWithOr(criteriaBuilder, alternatives);
    }

    private Optional<Predicate> uuidPredicate(CriteriaBuilder criteriaBuilder, Root<Patient> root, String value) {
        try {
            UUID uuid = UUID.fromString(value);
            return Optional.of(criteriaBuilder.equal(root.<UUID>get(FhirConstants.ID_PROPERTY), uuid));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * {@code patient.id IN (select pi.patientId from PatientIdentity pi [join type]
     * where pi.identityData = value)}.
     */
    private <R> Predicate identityPredicate(FhirCriteriaContext<Patient, R> context, String value,
            String identityType) {

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);
        FhirCriteriaContext<PatientIdentity, String> sub = createSubQuery(context, PatientIdentity.class, String.class);
        Subquery<String> subquery = sub.getSubquery();
        Root<PatientIdentity> identity = sub.getRoot();
        subquery.select(identity.get("patientId"));

        List<Predicate> where = new ArrayList<>();
        where.add(criteriaBuilder.equal(identity.get("identityData"), value));
        if (identityType != null) {
            Root<PatientIdentityType> type = subquery.from(PatientIdentityType.class);
            where.add(criteriaBuilder.equal(identity.get("identityTypeId"), type.get("id")));
            where.add(criteriaBuilder.equal(criteriaBuilder.upper(type.get("identityType")), identityType));
        }
        subquery.where(where.toArray(Predicate[]::new));

        return context.getRoot().get("id").in(subquery);
    }
}
