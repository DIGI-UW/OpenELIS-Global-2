package org.openelisglobal.labelpreset.service;

import java.util.Map;

/**
 * A label quantity outside the permitted range, with the text tag that says why
 * and the values that text names, such as the limit that was exceeded. Anything
 * else thrown while rendering is a real fault and must surface as one.
 */
public class LabelQuantityRefusedException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public static final String BELOW_MINIMUM = "error.labels.quantity.min";
    public static final String ABOVE_MAXIMUM = "error.labels.quantity.max";
    public static final String ABOVE_LIMIT = "barcode.label.error.quantity.limit";

    public static final String JOB_TOO_LARGE = "barcode.label.error.batch.limit";

    private final String messageKey;
    private final Map<String, Object> messageArgs;

    public LabelQuantityRefusedException(String messageKey, String message) {
        this(messageKey, message, Map.of());
    }

    public LabelQuantityRefusedException(String messageKey, String message, Map<String, Object> messageArgs) {
        super(message);
        this.messageKey = messageKey;
        this.messageArgs = Map.copyOf(messageArgs);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, Object> getMessageArgs() {
        return messageArgs;
    }
}
