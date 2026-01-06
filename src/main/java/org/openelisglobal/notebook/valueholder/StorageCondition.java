package org.openelisglobal.notebook.valueholder;

/**
 * Storage condition types for post-analysis sample storage. Defines temperature
 * ranges and display names for different storage requirements.
 *
 * <p>
 * Per US6: Store processed samples under defined conditions (refrigerated,
 * frozen, etc.)
 */
public enum StorageCondition {
    REFRIGERATED("Refrigerated (2-8°C)", "2-8°C"),

    FROZEN_MINUS20("Frozen (-20°C)", "-20°C"),

    FROZEN_MINUS80("Frozen (-80°C)", "-80°C"),

    ROOM_TEMP("Room Temperature (15-25°C)", "15-25°C"),

    LIQUID_NITROGEN("Liquid Nitrogen (-196°C)", "-196°C");

    private final String displayName;
    private final String temperatureRange;

    StorageCondition(String displayName, String temperatureRange) {
        this.displayName = displayName;
        this.temperatureRange = temperatureRange;
    }

    /**
     * Get user-friendly display name including temperature range.
     *
     * @return Display name like "Refrigerated (2-8°C)"
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get just the temperature range for display.
     *
     * @return Temperature range like "2-8°C"
     */
    public String getTemperatureRange() {
        return temperatureRange;
    }

    /**
     * Parse from string, case-insensitive.
     *
     * @param value String representation
     * @return StorageCondition or null if not found
     */
    public static StorageCondition fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
