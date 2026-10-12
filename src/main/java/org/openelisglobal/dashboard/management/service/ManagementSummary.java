package org.openelisglobal.dashboard.management.service;

import java.time.Instant;
import java.util.Map;
import org.openelisglobal.dashboard.management.ScopeRange;

public record ManagementSummary(String labName, long samplesReceived, Map<String, Boolean> features, ScopeRange scope,
        String generatedAt) {

    public ManagementSummary(String labName, long samplesReceived, Map<String, Boolean> features, ScopeRange scope) {
        this(labName, samplesReceived, features, scope, Instant.now().toString());
    }
}
