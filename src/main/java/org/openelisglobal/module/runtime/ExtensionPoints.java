package org.openelisglobal.module.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.spi.ExtensionPoint;
import org.openelisglobal.spi.report.ReportProvider;

/**
 * The extension points the SPI publishes, by the name a manifest uses under
 * {@code backend.extensions[].spi}, and whether each accepts several providers
 * at once or exactly one active provider.
 */
public final class ExtensionPoints {

    /** How many providers of a point may be active. */
    public enum Cardinality {
        /** Every enabled provider runs, in manifest order. */
        MULTIPLE,
        /**
         * One provider is active; {@code spi.<name>.provider} chooses when several are
         * installed.
         */
        SINGLE
    }

    public record Definition(String name, Class<? extends ExtensionPoint> type, Cardinality cardinality) {
    }

    private static final Map<String, Definition> BY_NAME = new LinkedHashMap<>();

    static {
        register(new Definition("report", ReportProvider.class, Cardinality.MULTIPLE));
    }

    private ExtensionPoints() {
    }

    private static void register(Definition definition) {
        BY_NAME.put(definition.name(), definition);
    }

    public static Set<String> names() {
        return Collections.unmodifiableSet(BY_NAME.keySet());
    }

    public static Definition definition(String name) {
        return BY_NAME.get(name);
    }

    /** The interface for a manifest name, or {@code null} for an unknown name. */
    public static Class<? extends ExtensionPoint> interfaceFor(String name) {
        Definition definition = BY_NAME.get(name);
        return definition == null ? null : definition.type();
    }

    /** The manifest name of an extension-point interface, or {@code null}. */
    public static String nameOf(Class<?> type) {
        for (Definition definition : BY_NAME.values()) {
            if (definition.type().equals(type)) {
                return definition.name();
            }
        }
        return null;
    }
}
