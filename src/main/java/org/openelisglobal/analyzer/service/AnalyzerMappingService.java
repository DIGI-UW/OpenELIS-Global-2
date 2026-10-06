package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;

public interface AnalyzerMappingService {

    AnalyzerMappingSnapshot resolveInitialRevision(AnalyzerProfileBinding profileBinding, JsonNode portableProfile,
            String actor);

    AnalyzerMappingSnapshot appendRevision(AnalyzerSiteBinding binding, AnalyzerMappingDraft draft, String actor);

    Optional<AnalyzerMappingSnapshot> findCurrentByProfileBindingId(String profileBindingId);

    Optional<AnalyzerMappingSnapshot> findByRevisionId(String revisionId);
}
