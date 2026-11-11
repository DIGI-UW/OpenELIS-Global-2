package org.openelisglobal.testcalculated.service;

import java.util.Map;

/**
 * A calculation that cannot be saved or switched on as configured. The message
 * key and its arguments are what the editor shows, so the person configuring
 * the rule learns which rule or test is in the way rather than that the save
 * failed.
 */
public class CalculationRuleViolation extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final Map<String, String> messageArgs;

    public CalculationRuleViolation(String messageKey, Map<String, String> messageArgs) {
        super(messageKey + " " + messageArgs);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, String> getMessageArgs() {
        return messageArgs;
    }
}
