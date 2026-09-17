package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerInstanceService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceView create(AnalyzerInstanceRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerInstanceState> list();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceView get(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceView update(String analyzerId, AnalyzerInstanceRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceView selectSiteBindingRevision(String analyzerId, String siteBindingId, int revision,
            String bindingFingerprint, String actor);
}
