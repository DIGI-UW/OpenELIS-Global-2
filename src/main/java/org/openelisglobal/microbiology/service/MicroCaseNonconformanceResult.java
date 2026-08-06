package org.openelisglobal.microbiology.service;

import java.util.List;

public record MicroCaseNonconformanceResult(String nceId, String nceNumber, String disposition, String eventType,
        List<String> affectedCaseIds) {
}
