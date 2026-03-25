package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;

public interface AnalyzerBidirectionalService {

    Map<String, Object> sendOrder(String analyzerId, String accessionNumber);

    Map<String, Object> queryResults(String analyzerId, String accessionNumber, List<String> testCodes,
            String sysUserId);
}
