package org.openelisglobal.labelpreset.service;

/**
 * A reprint quantity the service refuses (below one, or above the live preset's
 * maximum for the label's scope), with the text tag that says why. Anything
 * else thrown while rendering is a real fault and must surface as one.
 */
public class LabelQuantityRefusedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String BELOW_MINIMUM = "error.labels.quantity.min";
    public static final String ABOVE_MAXIMUM = "error.labels.quantity.max";

    private final String messageKey;

    public LabelQuantityRefusedException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
