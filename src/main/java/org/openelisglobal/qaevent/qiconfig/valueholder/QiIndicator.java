package org.openelisglobal.qaevent.qiconfig.valueholder;

/**
 * OGC-709 — the fixed inventory of quality indicators the QI dashboard tracks,
 * and the single source of truth for indicator identity + threshold direction.
 * The {@code qi_config} CHECK constraint and the shipped Liquibase seed are
 * derived from this enum; a fifth indicator means editing the enum + adding a
 * seed row (+ the CHECK line). Lab-defined custom indicators are a v2 concern,
 * which is why this is a code enum rather than a data-driven table.
 */
public enum QiIndicator {
    TAT(Direction.HIGHER_BETTER, true), REJECTION(Direction.LOWER_BETTER, true),
    AMENDMENT(Direction.LOWER_BETTER, true), NCE(Direction.LOWER_BETTER, false),
    CALLBACK(Direction.HIGHER_BETTER, false);

    /**
     * Whether a higher metric value is better (e.g. TAT % on-time) or worse (e.g.
     * rejection %). Drives threshold ordering validation and the breach comparison
     * that OGC-712 will apply.
     */
    public enum Direction {
        HIGHER_BETTER, LOWER_BETTER
    }

    private final Direction direction;
    private final boolean thresholdsRequired;

    QiIndicator(Direction direction, boolean thresholdsRequired) {
        this.direction = direction;
        this.thresholdsRequired = thresholdsRequired;
    }

    public Direction getDirection() {
        return direction;
    }

    /** NCE ships without numeric thresholds until OGC-712 defines its bands. */
    public boolean isThresholdsRequired() {
        return thresholdsRequired;
    }

    /** Parse a key to the enum, or {@code null} if it is not a known indicator. */
    public static QiIndicator fromKey(String key) {
        if (key == null) {
            return null;
        }
        try {
            return valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
