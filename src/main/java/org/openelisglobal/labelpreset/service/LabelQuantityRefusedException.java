package org.openelisglobal.labelpreset.service;

/**
 * A label quantity outside the permitted range, with the text tag that says
 * why. Anything else thrown while rendering is a real fault and must surface as
 * one.
 */
public class LabelQuantityRefusedException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public static final String BELOW_MINIMUM = "error.labels.quantity.min";
    public static final String ABOVE_MAXIMUM = "error.labels.quantity.max";

    public static final String JOB_TOO_LARGE = "barcode.label.error.batch.limit";

    private final String messageKey;

    public LabelQuantityRefusedException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
