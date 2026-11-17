package org.openelisglobal.program.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Reads a value a request posts by name, so a name the enum does not hold is
 * refused by the rule that needs the value rather than failing to bind.
 */
public final class EnumNames {

    private EnumNames() {
    }

    /** The constant of that name; null for a blank name or one the enum lacks. */
    public static <E extends Enum<E>> E valueNamed(Class<E> type, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            return Enum.valueOf(type, name.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
