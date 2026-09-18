package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerSiteBindingService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingSnapshot resolveInitialRevision(AnalyzerProfileBinding profileBinding, JsonNode portableProfile,
            String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerSiteBindingSnapshot appendRevision(AnalyzerSiteBinding binding, AnalyzerSiteBindingDraft draft,
            String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<AnalyzerSiteBindingSnapshot> findCurrentByProfileBindingId(String profileBindingId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<AnalyzerSiteBindingSnapshot> findByRevisionId(String revisionId);
}
