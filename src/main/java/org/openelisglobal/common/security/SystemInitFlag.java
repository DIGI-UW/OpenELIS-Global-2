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
}
