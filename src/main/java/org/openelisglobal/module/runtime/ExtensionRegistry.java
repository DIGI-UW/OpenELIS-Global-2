package org.openelisglobal.module.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.reports.action.implementation.ReportImplementationFactory;
import org.openelisglobal.spi.ExtensionPoint;
import org.openelisglobal.spi.report.ReportProvider;

/**
 * The extension-point implementations that modules declared and the runtime
 * registered, keyed by extension-point interface. Core seams consult it instead
 * of constructing implementations themselves: the report factory asks for the
 * provider of a report id, a future billing seam asks for the active billing
 * provider.
 *
 * <p>
 * Only beans declared under {@code backend.extensions} get here; an undeclared
 * implementation of an extension point inside a module is ignored. For a
 * single-implementation point with several providers installed, the
 * {@code spi.<name>.provider} property names the active one and start-up fails
 * without it.
 */
public final class ExtensionRegistry {

    /** One registered implementation. */
    public record Registration<T extends ExtensionPoint>(String moduleId, T instance, int order) {
    }

    private final Map<Class<? extends ExtensionPoint>, List<Registration<?>>> registrations = new LinkedHashMap<>();
    private final Map<String, String> reportIds = new LinkedHashMap<>();

    /**
     * Registers an implementation. For {@link ReportProvider} the report ids must
     * be unique across providers.
     *
     * @throws ModuleStartupException when two providers answer for the same report
     *                                id
     */
    public synchronized <T extends ExtensionPoint> void register(Class<T> point, String moduleId, T instance,
            int order) {
        if (instance instanceof ReportProvider provider) {
            for (String reportId : provider.reportIds()) {
                if (ReportImplementationFactory.isBuiltInReport(reportId)) {
                    throw new ModuleStartupException("report '" + reportId + "' is a core report; module '" + moduleId
                            + "' cannot take it over");
                }
                String other = reportIds.putIfAbsent(reportId, moduleId);
                if (other != null && !other.equals(moduleId)) {
                    throw new ModuleStartupException("report '" + reportId + "' is provided by both module '" + other
                            + "' and module '" + moduleId + "'; report ids must be unique");
                }
            }
        }
        registrations.computeIfAbsent(point, key -> new ArrayList<>())
                .add(new Registration<>(moduleId, instance, order));
    }

    /** Every registered implementation of a point, in manifest order. */
    @SuppressWarnings("unchecked")
    public synchronized <T extends ExtensionPoint> List<Registration<T>> registrations(Class<T> point) {
        List<Registration<?>> list = registrations.getOrDefault(point, List.of());
        List<Registration<T>> typed = new ArrayList<>();
        for (Registration<?> registration : list) {
            typed.add((Registration<T>) registration);
        }
        typed.sort(Comparator.comparingInt(Registration::order));
        return Collections.unmodifiableList(typed);
    }

    /** The instances of a point, in order. */
    public <T extends ExtensionPoint> List<T> providers(Class<T> point) {
        List<T> instances = new ArrayList<>();
        for (Registration<T> registration : registrations(point)) {
            instances.add(registration.instance());
        }
        return instances;
    }

    /** The provider that answers for a report id. */
    public synchronized Optional<ReportProvider> reportProvider(String reportId) {
        if (reportId == null) {
            return Optional.empty();
        }
        for (Registration<ReportProvider> registration : registrations(ReportProvider.class)) {
            if (registration.instance().reportIds().contains(reportId)) {
                return Optional.of(registration.instance());
            }
        }
        return Optional.empty();
    }

    /**
     * The active provider of a single-implementation point: the only one installed,
     * or the one {@code spi.<name>.provider} selects.
     *
     * @throws ModuleStartupException when several are installed and none is
     *                                selected
     */
    public <T extends ExtensionPoint> Optional<T> activeProvider(Class<T> point, Optional<String> selectedModuleId) {
        List<Registration<T>> list = registrations(point);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        if (selectedModuleId.isPresent()) {
            for (Registration<T> registration : list) {
                if (registration.moduleId().equals(selectedModuleId.get())) {
                    return Optional.of(registration.instance());
                }
            }
            throw new ModuleStartupException("spi." + ExtensionPoints.nameOf(point) + ".provider names module '"
                    + selectedModuleId.get() + "', which provides no " + point.getSimpleName());
        }
        if (list.size() > 1) {
            List<String> ids = new ArrayList<>();
            for (Registration<T> registration : list) {
                ids.add(registration.moduleId());
            }
            throw new ModuleStartupException("modules " + ids + " all provide " + point.getSimpleName() + "; set spi."
                    + ExtensionPoints.nameOf(point) + ".provider to the one to use");
        }
        return Optional.of(list.get(0).instance());
    }

    /** All registrations, for the registry endpoint. */
    public synchronized Map<Class<? extends ExtensionPoint>, List<Registration<?>>> all() {
        Map<Class<? extends ExtensionPoint>, List<Registration<?>>> copy = new LinkedHashMap<>();
        registrations.forEach((point, list) -> copy.put(point, List.copyOf(list)));
        return copy;
    }

    synchronized void clear() {
        registrations.clear();
        reportIds.clear();
    }
}
