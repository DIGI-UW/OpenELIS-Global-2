package org.openelisglobal.configuration.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-thread state of the catalog import that is running: the import run the
 * handlers write their unresolved references against, and the references a
 * lookup failed on since the last flush. Handlers run rows in their own
 * transactions, so a reference noticed inside a row is kept here and written
 * once the row's transaction has ended.
 */
public final class ImportRunContext {

    /**
     * A lookup the catalog could not satisfy: what kind of thing, spelled how,
     * where.
     */
    public record PendingReference(String referenceType, String referenceValue, String context) {
    }

    private static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<PendingReference>> PENDING = ThreadLocal.withInitial(ArrayList::new);

    private ImportRunContext() {
    }

    public static void setRunId(String runId) {
        if (runId == null) {
            RUN_ID.remove();
        } else {
            RUN_ID.set(runId);
        }
    }

    public static String getRunId() {
        return RUN_ID.get();
    }

    public static void addPending(String referenceType, String referenceValue, String context) {
        PENDING.get().add(new PendingReference(referenceType, referenceValue, context));
    }

    /** Hands over and clears the references noticed since the last call. */
    public static List<PendingReference> drainPending() {
        List<PendingReference> pending = new ArrayList<>(PENDING.get());
        PENDING.get().clear();
        return pending;
    }

    public static void clearPending() {
        PENDING.get().clear();
    }

    public static void clear() {
        RUN_ID.remove();
        PENDING.remove();
    }
}
