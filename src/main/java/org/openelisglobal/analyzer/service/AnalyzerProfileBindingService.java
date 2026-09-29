package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerProfileBindingService extends BaseObjectService<AnalyzerProfileBinding, String> {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerProfileBinding resolveActiveRevision(String profileId, int profileRevision, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerProfileBinding assignProfile(Analyzer analyzer, String profileId, int profileRevision, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    long getAnalyzerUsageCount(String bindingId);
}
