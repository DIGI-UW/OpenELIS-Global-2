package org.openelisglobal.compliance.service;

import org.openelisglobal.internationalization.MessageUtil;

/**
 * Centralised English defaults for compliance-module error strings.
 *
 * Throws across the compliance services route through this helper so the
 * canonical text is in one place and FRS S-01 §9 i18n keys (error.compliance*)
 * can be overridden per locale without code edits.
 *
 * Each method tries to resolve through {@link MessageUtil} and falls back to
 * the hard-coded English default if the message bundle isn't on the classpath
 * (notably in mock unit-test contexts where Spring isn't booted). The defaults
 * are kept verbatim with the strings tests currently assert on - localising
 * should not silently change exception messages that existing tests pin.
 */
public final class ComplianceMessages {

    private ComplianceMessages() {
    }

    private static String resolve(String key, Object[] args, String defaultMessage) {
        try {
            return MessageUtil.getMessageOrDefault(key, args, defaultMessage);
        } catch (Exception e) {
            // No Spring context (unit tests) or message source unavailable.
            // Fall back to the formatted English default so callers that
            // throw with this string keep their wording stable.
            return formatDefault(defaultMessage, args);
        }
    }

    private static String formatDefault(String defaultMessage, Object[] args) {
        if (args == null || args.length == 0) {
            return defaultMessage;
        }
        // Handle the simple {0}/{1} placeholders the bundle uses.
        String out = defaultMessage;
        for (int i = 0; i < args.length; i++) {
            out = out.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return out;
    }

    public static String standardNameRequired() {
        return resolve("error.compliance.standard.name.required", null, "Standard name is required");
    }

    public static String standardVersionRequired() {
        return resolve("error.compliance.standard.version.required", null, "Version is required");
    }

    public static String standardIssuingBodyRequired() {
        return resolve("error.compliance.standard.issuingBody.required", null, "Issuing body is required");
    }

    public static String standardRegulationNumberRequired() {
        return resolve("error.compliance.standard.regulationNumber.required", null, "Regulation number is required");
    }

    public static String standardCountryRegionRequired() {
        return resolve("error.compliance.standard.countryRegion.required", null, "Country/region is required");
    }

    public static String standardStatusRequired() {
        return resolve("error.compliance.standard.status.required", null, "Status is required");
    }

    public static String standardNotNull() {
        return resolve("error.compliance.standard.notNull", null, "Compliance standard cannot be null");
    }

    public static String standardDuplicate() {
        return resolve("error.compliance.standard.duplicate", null,
                "A compliance standard with this name, regulation number, and version already exists");
    }

    public static String standardCannotDeletePreSeeded() {
        return resolve("error.compliance.standard.cannotDeletePreSeeded", null,
                "Pre-seeded standards cannot be deleted");
    }

    public static String standardCannotDeleteWithThresholds() {
        return resolve("error.compliance.standard.cannotDeleteWithThresholds", null,
                "Standard has linked thresholds and cannot be deleted");
    }

    public static String standardSupersededRequiresReplacement() {
        return resolve("error.compliance.standard.supersededRequiresReplacement", null,
                "A SUPERSEDED standard must reference its replacement via supersededByStandard");
    }

    public static String standardSampleTypesRequiredForActive() {
        return resolve("error.compliance.standard.sampleTypesRequiredForActive", null,
                "At least one applicable sample type is required before activating a standard");
    }

    public static String thresholdInvalidRange() {
        return resolve("error.compliance.threshold.invalidRange", null, "minimum value cannot exceed maximum value");
    }

    public static String thresholdMinimumRequired() {
        return resolve("error.compliance.threshold.minimumRequired", null,
                "Minimum value is required for MINIMUM threshold type");
    }

    public static String thresholdMaximumRequired() {
        return resolve("error.compliance.threshold.maximumRequired", null,
                "Maximum value is required for MAXIMUM threshold type");
    }

    public static String thresholdTargetRequired() {
        return resolve("error.compliance.threshold.targetRequired", null,
                "Target value is required for EXACT threshold type");
    }

    public static String thresholdQualitativeRequired() {
        return resolve("error.compliance.threshold.qualitativeRequired", null,
                "Qualitative value is required for DESCRIPTIVE threshold type");
    }

    public static String thresholdSelectMappingRequired() {
        return resolve("error.compliance.threshold.selectMappingRequired", null,
                "At least one option mapping is required for SELECT_MAP threshold type");
    }

    public static String thresholdRangeBothRequired() {
        return resolve("error.compliance.threshold.rangeBothRequired", null,
                "Both minimum and maximum values are required for RANGE threshold type");
    }

    public static String thresholdBorderlineBothRequired() {
        return resolve("error.compliance.threshold.borderlineBothRequired", null,
                "Both minimum and maximum values are required for BORDERLINE threshold type");
    }

    public static String thresholdDuplicateInGroup(String parameterCode) {
        return resolve("error.compliance.threshold.duplicateInGroup", new Object[] { parameterCode },
                "Parameter " + parameterCode + " already exists in group");
    }

    public static String parameterGroupCannotDeleteWithThresholds() {
        return resolve("error.compliance.parameterGroup.cannotDeleteWithThresholds", null,
                "Cannot delete parameter group while thresholds reference it. Remove the thresholds first.");
    }
}
