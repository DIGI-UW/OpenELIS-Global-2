package org.openelisglobal.barcode.labeltype;

import java.util.Set;
import java.util.function.Function;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.labelpreset.service.LabelPresetService;
import org.openelisglobal.labelpreset.valueholder.LabelFieldKey;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Decides which content fields a legacy label (order, specimen, slide, block,
 * freezer) prints.
 *
 * <p>
 * Since OGC-1218 the administrator chooses a label's content fields on its
 * system preset, and the retired Barcode Configuration screen can no longer
 * change the 25 {@code site_information} element keys the legacy renderers used
 * to read. So the preset is the single source: a legacy label prints a field
 * when the matching system preset carries that field key. The legacy key is
 * consulted only where no preset answer exists (no preset of that name, a
 * preset that still carries Lab Number alone because the seed has not run, or
 * no application context at all), so a label never comes out blank by accident.
 */
public final class LabelFieldPolicy {

    public static final String ORDER = "Order Label";
    public static final String SPECIMEN = "Specimen Label";
    public static final String SLIDE = "Slide Label";
    public static final String BLOCK = "Block Label";
    public static final String FREEZER = "Freezer Label";

    private static final Function<String, Set<String>> CONTEXT_LOOKUP = LabelFieldPolicy::fieldsFromPresets;

    private static volatile Function<String, Set<String>> lookup = CONTEXT_LOOKUP;

    private LabelFieldPolicy() {
    }

    /**
     * The field keys the named system preset prints, or {@code null} when the
     * preset gives no answer and the legacy keys must decide.
     */
    public static Set<String> printedFields(String systemPresetName) {
        try {
            Set<String> fields = lookup.apply(systemPresetName);
            if (fields == null || fields.stream().allMatch(LabelFieldKey.LAB_NUMBER.name()::equals)) {
                return null;
            }
            return fields;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Whether a label prints {@code key}: by the preset's fields when
     * {@link #printedFields} answered, otherwise by the legacy
     * {@code site_information} key.
     */
    public static boolean prints(Set<String> presetFields, LabelFieldKey key, Property legacyKey) {
        if (presetFields != null) {
            return presetFields.contains(key.name());
        }
        return "true".equals(ConfigurationProperties.getInstance().getPropertyValue(legacyKey));
    }

    static void useLookup(Function<String, Set<String>> replacement) {
        lookup = replacement == null ? CONTEXT_LOOKUP : replacement;
    }

    private static Set<String> fieldsFromPresets(String systemPresetName) {
        LabelPresetService service = SpringContext.getBean(LabelPresetService.class);
        return service == null ? null : service.systemPresetFieldKeys(systemPresetName);
    }
}
