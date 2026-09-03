package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerProfileBindingService extends BaseObjectService<AnalyzerProfileBinding, String> {

    AnalyzerProfileBinding resolveActiveRevision(String profileId, int profileRevision, String sysUserId);

    AnalyzerProfileBinding assignProfile(Analyzer analyzer, String profileId, int profileRevision, String sysUserId);

    long getAnalyzerUsageCount(String bindingId);
}
