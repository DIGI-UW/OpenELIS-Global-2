package org.openelisglobal.analyzer.service;

import java.util.Map;

public interface AnalyzerSetupVerificationService {

    Map<String, Object> getVerificationStatus(String analyzerId);

    Map<String, Object> verifySetup(String analyzerId, Map<String, Object> request, String sysUserId);

    boolean isCurrentlyVerifiedAndReady(String analyzerId);
}
