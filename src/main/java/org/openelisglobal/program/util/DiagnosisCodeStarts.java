package org.openelisglobal.program.util;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

/**
 * The starts of diagnosis codes that mark a diagnosis as critical, as an
 * administrator types them into one setting: separated by commas, with case and
 * surrounding spaces ignored.
 */
public final class DiagnosisCodeStarts {

    private DiagnosisCodeStarts() {
    }

    /**
     * Whether the code starts with one of the laboratory's critical starts; the
     * setting is read on every call, so a change applies at once.
     */
    public static boolean isCritical(String code) {
        return matches(
                parse(ConfigurationProperties.getInstance().getPropertyValue(Property.PATHOLOGY_CRITICAL_RESULT_CODES)),
                code);
    }

    /**
     * The setting as upper-case starts, in the order typed; a missing setting and
     * empty items give none.
     */
    public static List<String> parse(String setting) {
        if (setting == null) {
            return List.of();
        }
        return Arrays.stream(setting.split(",")).map(String::trim).filter(start -> !start.isEmpty())
                .map(start -> start.toUpperCase(Locale.ROOT)).toList();
    }

    /**
     * Whether the code begins with one of the starts; a blank code or no starts
     * never match.
     */
    public static boolean matches(List<String> starts, String code) {
        if (StringUtils.isBlank(code) || starts == null || starts.isEmpty()) {
            return false;
        }
        String wanted = code.trim().toUpperCase(Locale.ROOT);
        return starts.stream().anyMatch(wanted::startsWith);
    }
}
