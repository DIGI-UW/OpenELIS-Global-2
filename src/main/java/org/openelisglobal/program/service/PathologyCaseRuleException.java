package org.openelisglobal.program.service;

/**
 * A case save asked for something the case cannot hold, so the caller is
 * answered, not the log.
 */
public class PathologyCaseRuleException extends RuntimeException {

    public PathologyCaseRuleException(String message) {
        super(message);
    }
}
