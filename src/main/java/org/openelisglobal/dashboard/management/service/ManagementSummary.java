package org.openelisglobal.dashboard.management.service;

import java.util.Map;

public record ManagementSummary(String labName, long samplesReceived, Map<String, Boolean> features) {
}
