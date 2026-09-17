package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.springframework.security.access.prepost.PreAuthorize;

/** Transactional owner of the OpenELIS portion of an analyzer instance. */
public interface AnalyzerInstanceLocalStateService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceState create(AnalyzerInstanceRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerInstanceState> list();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceState get(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceState update(String analyzerId, AnalyzerInstanceRequest request, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceState selectSiteBindingRevision(String analyzerId, String siteBindingId, int revision,
            String bindingFingerprint, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerInstanceState attachBridgeConnection(String analyzerId, String bridgeConnectionId, String actor);
}
