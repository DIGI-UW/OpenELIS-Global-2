package org.openelisglobal.organization.locations;

import org.openelisglobal.organization.locations.LocationsImportApi.Options;

/**
 * The mode and decisions of the import being run on this thread. The
 * organizations loader reads them whether it was reached from the import page,
 * the reload API or start-up; the last two carry no options and get Add &amp;
 * update with no decisions.
 */
public final class LocationsImportContext {

    private static final ThreadLocal<Options> OPTIONS = new ThreadLocal<>();

    private LocationsImportContext() {
    }

    public static void set(Options options) {
        OPTIONS.set(options);
    }

    public static Options options() {
        Options options = OPTIONS.get();
        return options == null ? Options.merge() : options;
    }

    public static void clear() {
        OPTIONS.remove();
    }
}
