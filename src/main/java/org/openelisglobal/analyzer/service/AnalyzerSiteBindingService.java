package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;

public interface AnalyzerSiteBindingService {

    AnalyzerSiteBindingSnapshot resolveInitialRevision(AnalyzerProfileBinding profileBinding, JsonNode portableProfile,
            String actor);

    AnalyzerSiteBindingSnapshot appendRevision(AnalyzerSiteBinding binding, AnalyzerSiteBindingDraft draft,
            String actor);
}
