package org.openelisglobal.audittrail.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.hibernate.LazyInitializationException;

/**
 * Renders arbitrary field values into the human-readable strings used by the
 * audit-trail history.
 *
 * <p>
 * Used both by {@code AuditTrailServiceImpl} when WRITING a history row
 * (turning entity-typed property values into XML payloads) and by
 * {@code SystemAuditEventRestController} when READING a history row back
 * (turning stale XML payloads and live entity snapshots into the same shape for
 * the API + CSV/PDF export). Centralized here so the two code paths can't drift
 * apart — if they did, the read path could produce display output that doesn't
 * match what was written, leaving users staring at mismatched audit rows.
 */
public final class AuditFieldStringifier {

    private static final String HASHCODE_PATTERN = ".+@[0-9a-fA-F]+$";
    private static final int MAX_RECURSION_DEPTH = 3;
    private static final String[] DISPLAY_GETTERS = { "getName", "getDescription", "getValue", "getDisplayName" };

    private AuditFieldStringifier() {
    }

    /**
     * Stringify a field value for the audit log. Strings, numbers, booleans, dates,
     * UUIDs, and enums are stringified directly. Entity references whose
     * {@code toString()} would otherwise return the default
     * {@code ClassName@hashcode} are unwrapped via the first available getter among
     * {@code getName}, {@code getDescription}, {@code getValue},
     * {@code getDisplayName}, {@code getId}; if nothing meaningful is found, an
     * empty string is returned rather than the unusable hashcode form.
     *
     * <p>
     * Recursion is bounded at depth {@value #MAX_RECURSION_DEPTH} so wrappers like
     * the legacy {@code ValueHolder} (which holds the real entity behind
     * {@code getValue()}) get unwrapped one level before we look for a display
     * getter on the real entity, without risking pathological cycles.
     */
    public static String stringify(Object value) {
        return stringify(value, 0);
    }

    private static String stringify(Object value, int depth) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof java.util.Date || value instanceof java.util.UUID
                || value instanceof java.time.temporal.Temporal) {
            return value.toString();
        }
        if (depth > MAX_RECURSION_DEPTH) {
            return sanitize(value.toString());
        }
        for (String getter : DISPLAY_GETTERS) {
            Object result = invokeNoArgGetterRaw(value, getter);
            if (result == null || result == value) {
                continue;
            }
            String s = result instanceof String ? ((String) result).trim() : stringify(result, depth + 1);
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        Object id = invokeNoArgGetterRaw(value, "getId");
        if (id != null) {
            String s = id instanceof String ? ((String) id).trim() : String.valueOf(id);
            if (!s.isEmpty()) {
                return s;
            }
        }
        return sanitize(value.toString());
    }

    /**
     * Replace strings that look like the default {@code ClassName@hexhashcode} with
     * an empty string. Used both as the fallback inside {@link #stringify(Object)}
     * and on pre-stored history payloads that were written before the entity-aware
     * stringifier was introduced (so old rows render cleanly in the new UI).
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        return value.matches(HASHCODE_PATTERN) ? "" : value;
    }

    private static Object invokeNoArgGetterRaw(Object value, String getterName) {
        try {
            Method m = value.getClass().getMethod(getterName);
            if (m.getReturnType() == void.class) {
                return null;
            }
            return m.invoke(value);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        } catch (LazyInitializationException e) {
            return null;
        }
    }
}
