package org.openelisglobal.search.dao;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.openelisglobal.common.fhir.dao.BaseFhirDao;
import org.openelisglobal.common.fhir.internals.FhirCriteriaContext;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.search.FhirPropertyResolver;
import org.springframework.stereotype.Repository;

@Transactional
@Repository
public class FacadeHelperDao extends BaseFhirDao {
    public FacadeHelperDao(FhirPropertyResolver propertyResover) {
        super(propertyResover);
    }

    /**
     * Implements the standard Practitioner.name search.
     *
     * <p>
     * Each supplied value is matched against both the given name and family name.
     * </p>
     *
     * <pre>
     * Practitioner?name=John
     *
     * person.firstName LIKE 'john%'
     * OR
     * person.lastName LIKE 'john%'
     * </pre>
     */
    public <R> Optional<Predicate> createNamePredicate(FhirCriteriaContext<?, R> context, StringAndListParam name) {

        if (name == null) {
            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        Expression<String> givenExpression = resolveStringExpression(context, FhirConstants.FIRST_NAME_SEARCH_HANDLER);

        Expression<String> familyExpression = resolveStringExpression(context, FhirConstants.LAST_NAME_SEARCH_HANDLER);

        return handleStringAndListParam(criteriaBuilder, name, parameter -> {

            Optional<Predicate> givenPredicate = createSingleStringPredicate(criteriaBuilder, givenExpression,
                    parameter);

            Optional<Predicate> familyPredicate = createSingleStringPredicate(criteriaBuilder, familyExpression,
                    parameter);

            return combineWithOr(criteriaBuilder,
                    Stream.of(givenPredicate, familyPredicate).flatMap(Optional::stream).toList());
        });
    }

    /**
     * Implements the standard Practitioner.telecom token search.
     *
     * <p>
     * Repeated token parameters are combined with AND semantics, while
     * comma-separated token values are combined with OR semantics.
     * </p>
     *
     * <p>
     * The token system may be used as a ContactPoint system selector:
     * {@code phone}, {@code email}, or {@code fax}. When no system is supplied, all
     * supported contact fields are searched.
     * </p>
     */
    public <R> Optional<Predicate> createTelecomPredicate(FhirCriteriaContext<?, R> context,
            TokenAndListParam telecom) {

        return createContactPointPredicate(context, telecom,
                List.of(FhirConstants.EMAIL_SEARCH_HANDLER, FhirConstants.WORK_PHONE_SEARCH_HANDLER,
                        FhirConstants.HOME_PHONE_SEARCH_HANDLER, FhirConstants.CELL_PHONE_SEARCH_HANDLER,
                        FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER, FhirConstants.FAX_SEARCH_HANDLER));
    }

    /**
     * Implements the standard Practitioner.email token search.
     */
    public <R> Optional<Predicate> createEmailPredicate(FhirCriteriaContext<?, R> context, TokenAndListParam email) {

        return createContactPointPredicate(context, email, List.of(FhirConstants.EMAIL_SEARCH_HANDLER));
    }

    /**
     * Implements the standard Practitioner.phone token search.
     *
     * <p>
     * Email and fax fields are excluded.
     * </p>
     */
    public <R> Optional<Predicate> createPhonePredicate(FhirCriteriaContext<?, R> context, TokenAndListParam phone) {

        return createContactPointPredicate(context, phone,
                List.of(FhirConstants.WORK_PHONE_SEARCH_HANDLER, FhirConstants.HOME_PHONE_SEARCH_HANDLER,
                        FhirConstants.CELL_PHONE_SEARCH_HANDLER, FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER));
    }

    /**
     * Builds a token predicate over one or more OpenELIS contact-point fields.
     *
     * <p>
     * Repeated token parameters are combined with AND, while comma-separated token
     * values are combined with OR.
     * </p>
     */
    public <R> Optional<Predicate> createContactPointPredicate(FhirCriteriaContext<?, R> context,
            TokenAndListParam tokenParam, List<String> configuredPropertyPaths) {

        if (tokenParam == null || configuredPropertyPaths == null || configuredPropertyPaths.isEmpty()) {

            return Optional.empty();
        }

        CriteriaBuilder criteriaBuilder = requireCriteriaBuilder(context);

        List<Predicate> andPredicates = new ArrayList<>();

        /*
         * TokenAndListParam:
         *
         * ?telecom=value1&telecom=value2 AND semantics
         *
         * ?telecom=value1,value2 OR semantics
         */
        tokenParam.getValuesAsQueryTokens().forEach(orList -> {

            List<Predicate> orPredicates = new ArrayList<>();

            /*
             * TokenOrListParam is not Iterable, so its token values must be obtained
             * through getValuesAsQueryTokens().
             */
            orList.getValuesAsQueryTokens().forEach(token -> {

                String system = normalize(token.getSystem());

                String value = normalize(token.getValue());

                if (value == null) {
                    return;
                }

                List<String> selectedPropertyPaths = selectContactPointProperties(system, configuredPropertyPaths);

                for (String propertyPath : selectedPropertyPaths) {

                    Expression<String> expression = resolveStringExpression(context, propertyPath);

                    Predicate valuePredicate = criteriaBuilder.equal(criteriaBuilder.lower(expression),
                            value.toLowerCase(Locale.ROOT));

                    orPredicates.add(valuePredicate);
                }
            });

            combineWithOr(criteriaBuilder, orPredicates).ifPresent(andPredicates::add);
        });

        if (andPredicates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(criteriaBuilder.and(andPredicates.toArray(Predicate[]::new)));
    }

    /**
     * Selects contact fields based on the optional ContactPoint system token.
     */
    private List<String> selectContactPointProperties(String system, List<String> configuredPropertyPaths) {

        if (system == null) {
            return configuredPropertyPaths;
        }

        return switch (system.toLowerCase(Locale.ROOT)) {
        case "email" -> configuredPropertyPaths.contains(FhirConstants.EMAIL_SEARCH_HANDLER)
                ? List.of(FhirConstants.EMAIL_SEARCH_HANDLER)
                : List.of();

        case "fax" -> configuredPropertyPaths.contains(FhirConstants.FAX_SEARCH_HANDLER)
                ? List.of(FhirConstants.FAX_SEARCH_HANDLER)
                : List.of();

        case "phone" -> configuredPropertyPaths.stream().filter(this::isPhoneProperty).toList();

        default -> List.of();
        };
    }

    private boolean isPhoneProperty(String propertyPath) {

        return FhirConstants.WORK_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.HOME_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.CELL_PHONE_SEARCH_HANDLER.equals(propertyPath)
                || FhirConstants.PRIMARY_PHONE_SEARCH_HANDLER.equals(propertyPath);
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
