package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.fhir.search.bundleProviders.PagedBundleProvider;

public final class FhirProviderUtils {

    private FhirProviderUtils() {
    }

    public static MethodOutcome buildCreateOutcome(IBaseResource resource) {

        MethodOutcome outcome = new MethodOutcome();

        outcome.setId(resource.getIdElement());
        outcome.setResource(resource);
        outcome.setCreated(true);
        outcome.setResponseStatusCode(201);

        return outcome;
    }

    public static MethodOutcome buildUpdateOutcome(IBaseResource resource) {

        MethodOutcome outcome = new MethodOutcome();

        outcome.setId(resource.getIdElement());
        outcome.setResource(resource);
        outcome.setCreated(false);
        outcome.setResponseStatusCode(200);

        return outcome;
    }

    public static MethodOutcome buildDeleteOutcome(IdType theId, String resourceType) {

        MethodOutcome outcome = new MethodOutcome();

        outcome.setId(theId);
        outcome.setResponseStatusCode(204);

        OperationOutcome operationOutcome = new OperationOutcome();

        operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setDiagnostics(resourceType + " " + theId.getIdPart() + " has been deleted");

        outcome.setOperationOutcome(operationOutcome);

        return outcome;
    }

    public static void syncToFhirStore(FhirPersistanceService fhirPersistenceService, Resource resource,
            String callerClassName, String callingMethod) {

        try {
            fhirPersistenceService.updateFhirResourceInFhirStore(resource);

        } catch (Exception exception) {
            LogEvent.logError(callerClassName, callingMethod,
                    "FHIR store sync failed (continuing anyway): " + exception.getMessage());
        }
    }

    public static String getSysUserId(HttpServletRequest request) {

        return ControllerUtills.getSysUserId(request);
    }

    public static String safeMessage(Exception e) {
        return (e == null || e.getMessage() == null) ? "No error message available" : e.getMessage();
    }

    /**
     * Tells a search result which page it is serving.
     *
     * <p>
     * Applied to every search so that the {@code next} link HAPI advertises
     * actually returns the next page. Results that do not page are left alone.
     *
     * @param result the bundle provider about to be returned to HAPI
     * @param offset zero-based {@code _offset}, null when the client sent none
     * @param count  {@code _count}, null when the client sent none
     * @return the same result, for use as a return expression
     */
    public static IBundleProvider withPaging(IBundleProvider result, Integer offset, Integer count) {
        if (result instanceof PagedBundleProvider provider) {
            provider.setCurrentPage(offset, count);
        }
        return result;
    }

    public static void validateIdParam(IdType theId, String resourceType, String callerClassName, String method) {

        if (theId == null || !theId.hasIdPart()) {

            LogEvent.logError(callerClassName, method, "Missing " + resourceType + " ID for " + method);

            throw new InvalidRequestException(resourceType + " ID must be provided for " + method);
        }
    }

    /**
     * Returns every AND group from a StringAndListParam.
     *
     * Example:
     *
     * given=John,James&given=Peter
     *
     * becomes:
     *
     * [ [John, James], [Peter] ]
     */
    public static List<List<StringParam>> stringParameterGroups(StringAndListParam parameter) {

        if (parameter == null || parameter.getValuesAsQueryTokens() == null
                || parameter.getValuesAsQueryTokens().isEmpty()) {

            return Collections.emptyList();
        }

        List<List<StringParam>> groups = new ArrayList<>();

        for (StringOrListParam orList : parameter.getValuesAsQueryTokens()) {

            if (orList == null || orList.getValuesAsQueryTokens() == null
                    || orList.getValuesAsQueryTokens().isEmpty()) {
                continue;
            }

            List<StringParam> values = new ArrayList<>();

            for (StringParam stringParam : orList.getValuesAsQueryTokens()) {

                if (stringParam == null || stringParam.getValue() == null || stringParam.getValue().isBlank()) {
                    continue;
                }

                values.add(stringParam);
            }

            if (!values.isEmpty()) {
                groups.add(values);
            }
        }

        return groups;
    }

    /**
     * Returns every AND group from a TokenAndListParam.
     *
     * Example:
     *
     * identifier=system|A,system|B&identifier=system|C
     *
     * becomes:
     *
     * [ [A, B], [C] ]
     */
    public static List<List<TokenParam>> tokenParameterGroups(TokenAndListParam parameter) {

        if (parameter == null || parameter.getValuesAsQueryTokens() == null
                || parameter.getValuesAsQueryTokens().isEmpty()) {

            return Collections.emptyList();
        }

        List<List<TokenParam>> groups = new ArrayList<>();

        for (TokenOrListParam orList : parameter.getValuesAsQueryTokens()) {

            if (orList == null || orList.getValuesAsQueryTokens() == null
                    || orList.getValuesAsQueryTokens().isEmpty()) {
                continue;
            }

            List<TokenParam> values = new ArrayList<>();

            for (TokenParam tokenParam : orList.getValuesAsQueryTokens()) {

                if (tokenParam == null) {
                    continue;
                }

                boolean hasValue = tokenParam.getValue() != null && !tokenParam.getValue().isBlank();

                boolean hasSystem = tokenParam.getSystem() != null && !tokenParam.getSystem().isBlank();

                if (hasValue || hasSystem) {
                    values.add(tokenParam);
                }
            }

            if (!values.isEmpty()) {
                groups.add(values);
            }
        }

        return groups;
    }

    /**
     * Use only when an operation explicitly supports one string value.
     *
     * Do not use this method for FHIR search processing.
     */
    public static String firstStringValue(StringAndListParam parameter) {

        List<List<StringParam>> groups = stringParameterGroups(parameter);

        if (groups.isEmpty() || groups.get(0).isEmpty()) {
            return null;
        }

        return groups.get(0).get(0).getValue();
    }

    /**
     * Use only when an operation explicitly supports one token.
     *
     * Do not use this method for FHIR search processing.
     */
    public static TokenParam firstToken(TokenAndListParam parameter) {

        List<List<TokenParam>> groups = tokenParameterGroups(parameter);

        if (groups.isEmpty() || groups.get(0).isEmpty()) {
            return null;
        }

        return groups.get(0).get(0);
    }

    public static String firstTokenValue(TokenAndListParam parameter) {

        TokenParam token = firstToken(parameter);

        return token == null ? null : token.getValue();
    }

    public static String firstTokenSystem(TokenAndListParam parameter) {

        TokenParam token = firstToken(parameter);

        return token == null ? null : token.getSystem();
    }

    public static UUID firstTokenUuid(TokenAndListParam parameter) {

        String value = firstTokenValue(parameter);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);

        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid UUID search value: " + value);
        }
    }

    public static Date lowerBoundFromDateRange(DateRangeParam parameter) {

        if (parameter == null || parameter.getLowerBound() == null) {
            return null;
        }

        return parameter.getLowerBound().getValue();
    }

    public static Date upperBoundFromDateRange(DateRangeParam parameter) {

        if (parameter == null || parameter.getUpperBound() == null) {
            return null;
        }

        return parameter.getUpperBound().getValue();
    }

    public static boolean hasValue(StringAndListParam parameter) {

        return !stringParameterGroups(parameter).isEmpty();
    }

    public static boolean hasValue(TokenAndListParam parameter) {

        return !tokenParameterGroups(parameter).isEmpty();
    }

    /**
     * Folds two spellings of the same reference search parameter (for example
     * {@code patient} and {@code subject}) into one AND list.
     */
    public static ReferenceAndListParam merge(ReferenceAndListParam first, ReferenceAndListParam second) {

        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        ReferenceAndListParam merged = new ReferenceAndListParam();
        first.getValuesAsQueryTokens().forEach(merged::addAnd);
        second.getValuesAsQueryTokens().forEach(merged::addAnd);
        return merged;
    }

    /**
     * True when the failure is the caller's data rather than a server fault: a
     * bean-validation violation, a database constraint or column-length rejection,
     * or an SQL data/integrity error anywhere in the cause chain.
     */
    public static boolean isDataError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof jakarta.validation.ConstraintViolationException
                    || current instanceof org.hibernate.exception.DataException
                    || current instanceof org.hibernate.exception.ConstraintViolationException
                    || current instanceof org.springframework.dao.DataIntegrityViolationException) {
                return true;
            }
            if (current instanceof java.sql.SQLException sqlException) {
                String state = sqlException.getSQLState();
                if (state != null && (state.startsWith("22") || state.startsWith("23"))) {
                    return true;
                }
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Maps a data error (see {@link #isDataError(Throwable)}) to a 422 whose
     * diagnostics name the rejected field or constraint instead of a bare 500.
     */
    public static UnprocessableEntityException unprocessableData(String resourceType, Throwable e) {
        Throwable current = e;
        String reason = null;
        while (current != null) {
            if (current instanceof jakarta.validation.ConstraintViolationException violation) {
                reason = violation.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage()).sorted()
                        .collect(java.util.stream.Collectors.joining("; "));
                break;
            }
            if (current instanceof java.sql.SQLException sqlException) {
                reason = describeSqlError(sqlException);
                break;
            }
            if (current.getCause() == null || current.getCause() == current) {
                reason = current.getMessage();
                break;
            }
            current = current.getCause();
        }
        if (reason == null || reason.isBlank()) {
            reason = e.getClass().getSimpleName();
        }
        return new UnprocessableEntityException(resourceType + " could not be stored: " + reason.trim());
    }

    /**
     * Reduces a JDBC failure to the database's own complaint.
     *
     * <p>
     * A batched insert reports itself as {@code BatchUpdateException}, whose
     * message is the entire generated statement with every bound value inlined and
     * the real cause appended at the end. Returning that verbatim gave callers a
     * diagnostics string thousands of characters long that buried the one sentence
     * explaining what was wrong, so the driver's chained exception is preferred and
     * the statement text dropped.
     */
    private static String describeSqlError(java.sql.SQLException e) {
        java.sql.SQLException deepest = e;
        java.sql.SQLException next = e.getNextException();
        while (next != null && next != deepest) {
            deepest = next;
            next = next.getNextException();
        }
        String message = deepest.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getMessage();
        }
        if (message == null) {
            return null;
        }
        int aborted = message.lastIndexOf("was aborted: ");
        if (aborted >= 0) {
            message = message.substring(aborted + "was aborted: ".length());
        }
        int callNext = message.indexOf("Call getNextException");
        if (callNext >= 0) {
            message = message.substring(0, callNext);
        }
        return message.trim();
    }
}