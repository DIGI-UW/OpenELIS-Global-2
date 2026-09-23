package org.openelisglobal.common.security;

import java.util.function.Supplier;

/**
 * Runs a block in system context, so the service-layer {@code @PreAuthorize}
 * gates it crosses are satisfied without the caller's own privileges.
 *
 * <p>
 * This is for <b>infrastructure assembly</b> — work that reads across several
 * admin-scoped services to build something the caller is already entitled to
 * see, where gating the individual reads would deny a user whose access to the
 * endpoint itself is not in question. Reference-data caches
 * ({@code DisplayListService}) and landing-page summary counts
 * ({@code PatientDashBoardProvider}) are the cases in hand.
 *
 * <p>
 * It is <b>not</b> a way to make an ungated endpoint reachable. The caller's
 * own access must still be gated where the request arrives; this only stops an
 * internal read from denying them halfway through. Restores rather than clears
 * on exit, so nesting inside an existing system-context block is safe.
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
