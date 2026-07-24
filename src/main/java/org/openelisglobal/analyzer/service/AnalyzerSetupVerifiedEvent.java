package org.openelisglobal.analyzer.service;

import org.springframework.context.ApplicationEvent;

public class AnalyzerSetupVerifiedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String analyzerId;

    public AnalyzerSetupVerifiedEvent(Object source, String analyzerId) {
        super(source);
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }
}
