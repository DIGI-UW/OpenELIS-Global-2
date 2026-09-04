package org.openelisglobal.common.security;

public final class SystemInitFlag {

    private static final ThreadLocal<Boolean> FLAG = ThreadLocal.withInitial(() -> false);

    private SystemInitFlag() {
    }

    public static void set() {
        FLAG.set(true);
    }

    public static void clear() {
        FLAG.remove();
    }

    public static boolean isSet() {
        return Boolean.TRUE.equals(FLAG.get());
    }

    /**
     * Enter system context, returning whether it was ALREADY active so the caller
     * can restore rather than blindly clear — clearing unconditionally would wipe
     * the startup flag when system-context work nests inside context initialization
     * (e.g. a cache refresh triggered from another bean's {@code @PostConstruct}).
     *
     * <pre>
     * boolean wasSet = SystemInitFlag.enter();
     * try {
     *     ...
     * } finally {
     *     SystemInitFlag.exit(wasSet);
     * }
     * </pre>
     */
    public static boolean enter() {
        boolean wasSet = isSet();
        FLAG.set(true);
        return wasSet;
    }

    public static void exit(boolean wasSet) {
        if (!wasSet) {
            FLAG.remove();
        }
    }
}
