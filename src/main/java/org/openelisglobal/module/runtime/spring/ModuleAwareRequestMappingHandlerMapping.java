package org.openelisglobal.module.runtime.spring;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The root handler mapping with one addition: module controllers, which live in
 * child contexts the dispatcher never looks into, can be registered explicitly.
 * A registered handler is dispatched exactly like a core controller, with the
 * same interceptors, argument resolvers, message converters and controller
 * advice, because it is the same mapping. While a module's controller is being
 * registered every mapping is checked against the module's REST prefix; one
 * outside it refuses the whole module.
 */
public class ModuleAwareRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private final ThreadLocal<String> registeringPrefix = new ThreadLocal<>();
    private final ThreadLocal<List<String>> registeredPaths = new ThreadLocal<>();

    /**
     * Registers every handler method of a module controller bean.
     *
     * @param handler    the controller bean (a proxy is fine; the user class is
     *                   read through it)
     * @param restPrefix the module's prefix, {@code /rest/modules/<id>/}
     * @return the paths registered
     * @throws ModuleStartupException when a mapping lies outside the prefix
     */
    public synchronized List<String> registerModuleController(Object handler, String restPrefix) {
        registeringPrefix.set(restPrefix);
        registeredPaths.set(new ArrayList<>());
        try {
            detectHandlerMethods(handler);
            return List.copyOf(registeredPaths.get());
        } finally {
            registeringPrefix.remove();
            registeredPaths.remove();
        }
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        String prefix = registeringPrefix.get();
        if (prefix != null) {
            Set<String> patterns = mapping.getPatternValues();
            if (patterns.isEmpty()) {
                throw new ModuleStartupException(
                        "controller method " + method + " has no path; module mappings" + " must be under " + prefix);
            }
            for (String pattern : patterns) {
                String withoutLeadingSlash = pattern.startsWith("/") ? pattern : "/" + pattern;
                if (!withoutLeadingSlash.startsWith(prefix)) {
                    throw new ModuleStartupException("controller method " + method + " is mapped to '" + pattern
                            + "', outside the module prefix " + prefix);
                }
                registeredPaths.get().add(withoutLeadingSlash);
            }
        }
        super.registerHandlerMethod(handler, method, mapping);
    }
}
