package org.openelisglobal.common.security;

import java.util.function.Supplier;

/**
 * Runs a block in system context, so the service-layer {@code @PreAuthorize}
 * gates it crosses are satisfied without the caller's own privileges.
 *
 * <p>
 * This is for <b>system actions</b>: work the application performs as a
 * consequence of something the caller was permitted to do, but which the caller
 * themselves has no privilege to perform. Routing an order onto the
 * microbiology bench ({@code micro:bench}), typing a referring site the order
 * just created ({@code organization:manage}), recording label quantities
 * ({@code barcode:manage}) and notifying results staff of a STAT order
 * ({@code system_user:view}) are the cases in hand. The caller places the
 * order; the system does the bookkeeping that follows.
 *
 * <p>
 * It is <b>not</b> the answer to "an order-entry role cannot read reference
 * data". That was the original motivation for most of the call sites here, and
 * it was the wrong fix: it turned an authorization problem into a bypass, at
 * ~44 sites, each one silently exempt from review. Reading the orderable
 * catalogue, sample types, tests, panels, methods, units, dictionaries,
 * programmes, projects, is now an explicit privilege,
 * {@code PRIV_CATALOGUE_VIEW}, granted to the roles that need it. If a new read
 * denies, widen the gate to accept that privilege rather than wrapping the call
 * here.
 *
 * <p>
 * It is also <b>not</b> a way to make an ungated endpoint reachable. The
 * caller's own access must still be gated where the request arrives; this only
 * stops an internal write from denying them halfway through. Restores rather
 * than clears on exit, so nesting inside an existing system-context block is
 * safe.
 *
 * <p>
 * Prefer the daemon identity ({@code DaemonContextExecutor}) for work with no
 * human caller at all — it is a real principal and shows up in auditing, which
 * this deliberately does not.
 */
public final class SystemContext {

    private SystemContext() {
    }

    /** Runs {@code work} in system context and returns its result. */
    public static <T> T callAsSystem(Supplier<T> work) {
        boolean wasSet = SystemInitFlag.enter();
        try {
            return work.get();
        } finally {
            SystemInitFlag.exit(wasSet);
        }
    }

    /** Runs {@code work} in system context. */
    public static void runAsSystem(Runnable work) {
        boolean wasSet = SystemInitFlag.enter();
        try {
            work.run();
        } finally {
            SystemInitFlag.exit(wasSet);
        }
    }
}
