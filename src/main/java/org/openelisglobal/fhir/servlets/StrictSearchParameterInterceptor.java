package org.openelisglobal.fhir.servlets;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.method.SearchMethodBinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;

/**
 * Rejects a search the facade would otherwise answer wrongly.
 *
 * <p>
 * HAPI's method binding answers 400 for an unknown parameter name, but it skips
 * any name starting with an underscore, accepts the {@code :missing} modifier
 * and chained parameters on every type, and hands a token with a system and no
 * code to the DAOs, which drop what they cannot read. Each of those used to
 * return every resource of the type, indistinguishable from a real match. This
 * interceptor checks a type search against what the provider's {@link Search}
 * method declares and what the DAOs implement, and answers 400 with an
 * OperationOutcome naming the parameter instead:
 * <ul>
 * <li>an underscore parameter the method does not declare, other than the
 * response-shaping ones HAPI applies itself ({@code _format}, {@code _pretty},
 * {@code _summary}, {@code _elements}, {@code _total});</li>
 * <li>{@code _sort}, which no DAO applies;</li>
 * <li>a chained parameter ({@code subject.name});</li>
 * <li>any modifier other than {@code :exact} and {@code :contains} on a string
 * parameter;</li>
 * <li>a token with a system and no code, except on the contact-point parameters
 * that search by system alone;</li>
 * <li>a date prefix other than {@code eq}, {@code gt}, {@code ge}, {@code lt}
 * and {@code le};</li>
 * <li>a code outside the required value set of a coded parameter
 * ({@code gender=xyz}, {@code status=bogus}, {@code active=maybe});</li>
 * <li>a reference to a type that is not a FHIR resource, or that the parameter
 * cannot point at, or that names no id ({@code subject=Patient/});</li>
 * <li>a malformed token ({@code code=a|b|c}) and a negative
 * {@code _count}.</li>
 * </ul>
 * An empty value is still ignored, as the FHIR search rules require. A client
 * that prefers the old behaviour sends {@code Prefer: handling=lenient}, the
 * standard FHIR request for a lenient server: parameters that cannot be applied
 * are then dropped instead of rejected, and values are not checked. HAPI's own
 * {@code SearchPreferHandlingInterceptor} is not used, because it skips
 * underscore names and removes a modified parameter ({@code gender:missing}) by
 * its bare name, so the lenient request still failed.
 */
@Interceptor
public class StrictSearchParameterInterceptor {

    enum Kind {
        STRING, TOKEN, REFERENCE, DATE, CONTROL, OTHER
    }

    static final Set<String> RESPONSE_PARAMETERS = Set.of("_format", "_pretty", "_summary", "_elements", "_total");

    static final Set<String> STRING_MODIFIERS = Set.of("exact", "contains");

    static final Set<String> SYSTEM_ONLY_TOKENS = Set.of("telecom", "email", "phone");

    static final Set<String> DATE_PREFIXES = Set.of("eq", "gt", "ge", "lt", "le");

    /**
     * Token parameters bound to a required FHIR value set, with the codes the value
     * set allows, read from HAPI's R4 model enums. An invalid code used to answer
     * an empty bundle, indistinguishable from no match.
     */
    static final Map<String, Set<String>> CODED_PARAMETERS = Map.of("Patient.gender",
            codes(Enumerations.AdministrativeGender.values()), "ServiceRequest.status",
            codes(ServiceRequest.ServiceRequestStatus.values()), "Specimen.status",
            codes(Specimen.SpecimenStatus.values()), "Observation.status",
            codes(Observation.ObservationStatus.values()), "DiagnosticReport.status",
            codes(DiagnosticReport.DiagnosticReportStatus.values()), "Location.status",
            codes(Location.LocationStatus.values()), "Device.status", codes(Device.FHIRDeviceStatus.values()),
            "Organization.active", Set.of("true", "false"));

    private static Set<String> codes(Enum<?>[] constants) {
        Set<String> codes = new HashSet<>();
        for (Enum<?> constant : constants) {
            if ("NULL".equals(constant.name())) {
                continue;
            }
            try {
                codes.add((String) constant.getClass().getMethod("toCode").invoke(constant));
            } catch (ReflectiveOperationException e) {
                codes.add(constant.name().toLowerCase(Locale.ROOT).replace('_', '-'));
            }
        }
        return Set.copyOf(codes);
    }

    private final Map<Class<?>, Map<String, Kind>> declaredByProvider = new ConcurrentHashMap<>();

    /**
     * Runs before HAPI picks the search method, so a lenient request can have its
     * unsupported parameters removed before the method binding refuses them.
     */
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
    public void checkSearch(RequestDetails requestDetails) {
        if (!SearchMethodBinding.isPlainSearchRequest(requestDetails)) {
            return;
        }
        IResourceProvider provider = findProvider(requestDetails);
        if (provider == null) {
            return;
        }
        Map<String, Kind> declared = declaredByProvider.computeIfAbsent(provider.getClass(),
                StrictSearchParameterInterceptor::declaredParameters);
        if (declared.isEmpty()) {
            return;
        }
        String resourceName = requestDetails.getResourceName();
        if (isLenient(requestDetails)) {
            Map<String, String[]> supported = new LinkedHashMap<>();
            requestDetails.getParameters().forEach((rawName, values) -> {
                if (unsupported(resourceName, declared, rawName, values) == null) {
                    supported.put(rawName, values);
                }
            });
            if (supported.size() != requestDetails.getParameters().size()) {
                requestDetails.setParameters(supported);
            }
            return;
        }
        for (Map.Entry<String, String[]> parameter : requestDetails.getParameters().entrySet()) {
            check(requestDetails.getFhirContext(), resourceName, declared, parameter.getKey(), parameter.getValue());
        }
    }

    /**
     * Why a parameter cannot be applied at all (an unknown name, a chain, an
     * unsupported modifier, {@code _sort}), or null when it can. A strict search
     * rejects with this message; a lenient one drops the parameter.
     */
    static String unsupported(String resourceName, Map<String, Kind> declared, String rawName, String[] values) {
        if (RESPONSE_PARAMETERS.contains(rawName)) {
            return null;
        }
        if (rawName.contains(".")) {
            return "Chained search parameter '" + rawName + "' is not supported for " + resourceName;
        }
        int colon = rawName.indexOf(':');
        String name = colon < 0 ? rawName : rawName.substring(0, colon);
        String modifier = colon < 0 ? null : rawName.substring(colon + 1);
        Kind kind = declared.get(name);
        if (kind == null) {
            return "Unknown search parameter '" + name + "' for " + resourceName + ". Supported parameters: "
                    + String.join(", ", new TreeMap<>(declared).keySet());
        }
        if ("_sort".equals(name) && hasValue(values)) {
            return "The _sort parameter is not supported for " + resourceName
                    + ": results are returned in a stable server order";
        }
        if (modifier != null && !(kind == Kind.STRING && STRING_MODIFIERS.contains(modifier))) {
            return "Modifier ':" + modifier + "' is not supported on search parameter '" + name + "' for "
                    + resourceName;
        }
        return null;
    }

    static void check(FhirContext fhirContext, String resourceName, Map<String, Kind> declared, String rawName,
            String[] values) {
        String reason = unsupported(resourceName, declared, rawName, values);
        if (reason != null) {
            throw reject(reason);
        }
        if (RESPONSE_PARAMETERS.contains(rawName)) {
            return;
        }
        int colon = rawName.indexOf(':');
        String name = colon < 0 ? rawName : rawName.substring(0, colon);
        Kind kind = declared.get(name);
        if ("_count".equals(name)) {
            checkCount(resourceName, values);
        }
        for (String value : splitValues(values)) {
            if (kind == Kind.TOKEN) {
                checkToken(resourceName, name, value);
                checkCode(resourceName, name, value);
            } else if (kind == Kind.REFERENCE) {
                checkReference(fhirContext, resourceName, name, value);
            } else if (kind == Kind.DATE) {
                checkDate(resourceName, name, value);
            }
        }
    }

    private static void checkToken(String resourceName, String name, String value) {
        int bar = unescapedBar(value);
        if (bar < 0) {
            return;
        }
        if (unescapedBar(value.substring(bar + 1)) >= 0) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " has a malformed token '" + value
                    + "': expected [system|]code");
        }
        String system = value.substring(0, bar).trim();
        String code = value.substring(bar + 1).trim();
        if (!code.isEmpty()) {
            return;
        }
        if (system.isEmpty()) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " has neither a system nor a code");
        }
        if (!SYSTEM_ONLY_TOKENS.contains(name)) {
            throw reject("Search parameter '" + name + "' for " + resourceName
                    + " needs a code: searching by system alone ('" + value + "') is not supported");
        }
    }

    private static void checkCode(String resourceName, String name, String value) {
        Set<String> allowed = CODED_PARAMETERS.get(resourceName + "." + name);
        if (allowed == null) {
            return;
        }
        int bar = unescapedBar(value);
        String code = (bar < 0 ? value : value.substring(bar + 1)).trim();
        if (!code.isEmpty() && !allowed.contains(code)) {
            throw reject("'" + code + "' is not a valid code for search parameter '" + name + "' on " + resourceName
                    + ". Valid codes: " + String.join(", ", new TreeSet<>(allowed)));
        }
    }

    /**
     * A reference value names an id, optionally preceded by a resource type or a
     * full URL. A type that is not a FHIR resource, or one the search parameter
     * cannot point at according to the FHIR definition HAPI carries for it, used to
     * be read as a bare id and match nothing; a type with no id after it
     * ({@code subject=Patient/}) dropped the constraint and matched everything.
     */
    private static void checkReference(FhirContext fhirContext, String resourceName, String name, String value) {
        if (value.endsWith("/")) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " names no id in '" + value + "'");
        }
        if (!value.contains("/")) {
            return;
        }
        String[] segments = value.split("/");
        int history = Arrays.asList(segments).indexOf("_history");
        int end = history > 0 ? history : segments.length;
        if (end < 2) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " has a malformed reference '" + value
                    + "'");
        }
        String type = segments[end - 2];
        if (fhirContext == null) {
            return;
        }
        if (!fhirContext.getResourceTypes().contains(type)) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " references '" + type
                    + "', which is not a FHIR resource type");
        }
        RuntimeSearchParam definition = fhirContext.getResourceDefinition(resourceName).getSearchParam(name);
        if (definition != null && !definition.getTargets().isEmpty() && !definition.getTargets().contains(type)) {
            throw reject("Search parameter '" + name + "' for " + resourceName + " cannot reference a " + type
                    + ". Allowed types: " + String.join(", ", new TreeSet<>(definition.getTargets())));
        }
    }

    private static void checkCount(String resourceName, String[] values) {
        for (String value : splitValues(values)) {
            try {
                if (Integer.parseInt(value) < 0) {
                    throw reject("_count must be zero or greater for " + resourceName + ", got '" + value + "'");
                }
            } catch (NumberFormatException e) {
                throw reject("_count must be a whole number for " + resourceName + ", got '" + value + "'");
            }
        }
    }

    private static void checkDate(String resourceName, String name, String value) {
        if (value.length() > 2 && Character.isLetter(value.charAt(0)) && Character.isLetter(value.charAt(1))) {
            String prefix = value.substring(0, 2).toLowerCase(Locale.ROOT);
            if (!DATE_PREFIXES.contains(prefix)) {
                throw reject("Date prefix '" + prefix + "' is not supported on search parameter '" + name + "' for "
                        + resourceName + ". Supported prefixes: eq, gt, ge, lt, le");
            }
        }
    }

    static List<String> splitValues(String[] values) {
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> split = new ArrayList<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\\' && i + 1 < value.length()) {
                    current.append(c).append(value.charAt(++i));
                } else if (c == ',') {
                    addIfPresent(split, current);
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            addIfPresent(split, current);
        }
        return split;
    }

    private static void addIfPresent(List<String> split, StringBuilder value) {
        if (!value.toString().trim().isEmpty()) {
            split.add(value.toString().trim());
        }
    }

    private static int unescapedBar(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '|') {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasValue(String[] values) {
        return !splitValues(values).isEmpty();
    }

    private static boolean isLenient(RequestDetails requestDetails) {
        List<String> prefer = requestDetails.getHeaders("Prefer");
        return prefer != null && prefer.stream().filter(header -> header != null)
                .anyMatch(header -> header.replace(" ", "").toLowerCase(Locale.ROOT).contains("handling=lenient"));
    }

    private static IResourceProvider findProvider(RequestDetails requestDetails) {
        if (!(requestDetails.getServer() instanceof RestfulServer server) || requestDetails.getResourceName() == null) {
            return null;
        }
        for (IResourceProvider provider : server.getResourceProviders()) {
            if (requestDetails.getResourceName()
                    .equals(server.getFhirContext().getResourceType(provider.getResourceType()))) {
                return provider;
            }
        }
        return null;
    }

    /**
     * The parameters every {@link Search} method of the provider declares, each
     * with the kind of value it takes. The provider class may be a Spring proxy, so
     * the annotations are read from the first class up the hierarchy that declares
     * a search.
     */
    static Map<String, Kind> declaredParameters(Class<?> providerClass) {
        Map<String, Kind> declared = new TreeMap<>();
        for (Class<?> type = providerClass; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Search.class)) {
                    addMethodParameters(method, declared);
                }
            }
            if (!declared.isEmpty()) {
                break;
            }
        }
        return declared;
    }

    private static void addMethodParameters(Method method, Map<String, Kind> declared) {
        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < types.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof OptionalParam optional) {
                    declared.put(optional.name(), kindOf(types[i]));
                } else if (annotation instanceof RequiredParam required) {
                    declared.put(required.name(), kindOf(types[i]));
                } else if (annotation instanceof IncludeParam include) {
                    declared.put(include.reverse() ? "_revinclude" : "_include", Kind.CONTROL);
                } else if (annotation instanceof Sort) {
                    declared.put("_sort", Kind.CONTROL);
                } else if (annotation instanceof Offset) {
                    declared.put("_offset", Kind.CONTROL);
                } else if (annotation instanceof Count) {
                    declared.put("_count", Kind.CONTROL);
                }
            }
        }
    }

    private static Kind kindOf(Class<?> type) {
        if (type == StringParam.class || type == StringOrListParam.class || type == StringAndListParam.class) {
            return Kind.STRING;
        }
        if (type == TokenParam.class || type == TokenOrListParam.class || type == TokenAndListParam.class) {
            return Kind.TOKEN;
        }
        if (type == DateParam.class || type == DateOrListParam.class || type == DateAndListParam.class
                || type == DateRangeParam.class) {
            return Kind.DATE;
        }
        if (type.getSimpleName().startsWith("Reference")) {
            return Kind.REFERENCE;
        }
        return Kind.OTHER;
    }

    private static InvalidRequestException reject(String message) {
        return new InvalidRequestException(message);
    }
}
