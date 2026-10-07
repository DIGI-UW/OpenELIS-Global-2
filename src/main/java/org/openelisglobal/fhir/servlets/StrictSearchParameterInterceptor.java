package org.openelisglobal.fhir.servlets;

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
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rejects a search the facade would otherwise answer wrongly.
 *
 * <p>
 * HAPI's own {@code SearchPreferHandlingInterceptor}, registered beside this
 * one, checks plain parameter names against the providers, but it skips every
 * name starting with an underscore, strips modifiers and chains before looking
 * a name up, and never reads a value. The method binding likewise answers 400
 * for an unknown parameter name, but it skips any name starting with an
 * underscore, accepts the {@code :missing} modifier and chained parameters on
 * every type, and hands a token with a system and no code to the DAOs, which
 * drop what they cannot read. Each of those used to return every resource of
 * the type, indistinguishable from a real match. This interceptor checks a type
 * search against what the provider's {@link Search} method declares and what
 * the DAOs implement, and answers 400 with an OperationOutcome naming the
 * parameter instead:
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
 * and {@code le}.</li>
 * </ul>
 * An empty value is still ignored, as the FHIR search rules require. A client
 * that prefers the old behaviour sends {@code Prefer: handling=lenient}, the
 * standard FHIR request for a lenient server, and the checks are skipped.
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

    private final Map<Class<?>, Map<String, Kind>> declaredByProvider = new ConcurrentHashMap<>();

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void checkSearch(RequestDetails requestDetails, RestOperationTypeEnum operation) {
        if (operation != RestOperationTypeEnum.SEARCH_TYPE || isLenient(requestDetails)) {
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
        for (Map.Entry<String, String[]> parameter : requestDetails.getParameters().entrySet()) {
            check(resourceName, declared, parameter.getKey(), parameter.getValue());
        }
    }

    static void check(String resourceName, Map<String, Kind> declared, String rawName, String[] values) {
        if (RESPONSE_PARAMETERS.contains(rawName)) {
            return;
        }
        if (rawName.contains(".")) {
            throw reject("Chained search parameter '" + rawName + "' is not supported for " + resourceName);
        }
        int colon = rawName.indexOf(':');
        String name = colon < 0 ? rawName : rawName.substring(0, colon);
        String modifier = colon < 0 ? null : rawName.substring(colon + 1);
        Kind kind = declared.get(name);
        if (kind == null) {
            throw reject("Unknown search parameter '" + name + "' for " + resourceName + ". Supported parameters: "
                    + String.join(", ", new TreeMap<>(declared).keySet()));
        }
        if ("_sort".equals(name) && hasValue(values)) {
            throw reject("The _sort parameter is not supported for " + resourceName
                    + ": results are returned in a stable server order");
        }
        if (modifier != null && !(kind == Kind.STRING && STRING_MODIFIERS.contains(modifier))) {
            throw reject("Modifier ':" + modifier + "' is not supported on search parameter '" + name + "' for "
                    + resourceName);
        }
        for (String value : splitValues(values)) {
            if (kind == Kind.TOKEN) {
                checkToken(resourceName, name, value);
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
