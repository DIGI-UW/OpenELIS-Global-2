package org.openelisglobal.analyzer.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerInstanceLocalStateServiceImpl implements AnalyzerInstanceLocalStateService {

    private final AnalyzerService analyzerService;
    private final AnalyzerProfileBindingService profileBindingService;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerResultsService analyzerResultsService;

    @Autowired
    public AnalyzerInstanceLocalStateServiceImpl(AnalyzerService analyzerService,
            AnalyzerProfileBindingService profileBindingService, AnalyzerSiteBindingService siteBindingService,
            AnalyzerResultsService analyzerResultsService) {
        this.analyzerService = analyzerService;
        this.profileBindingService = profileBindingService;
        this.siteBindingService = siteBindingService;
        this.analyzerResultsService = analyzerResultsService;
    }

    @Override
    @Transactional
    public AnalyzerInstanceState create(AnalyzerInstanceRequest request, String actor) {
        if (request == null) {
            throw new IllegalArgumentException("Analyzer request is required");
        }
        String exactActor = requireText(actor, "actor");
        String name = requireText(request.getName(), "Analyzer name");
        String profileId = requireText(request.getProfileId(), "Profile ID");
        int profileRevision = request.getProfileRevision() == null ? 0 : request.getProfileRevision();
        if (profileRevision < 1) {
            throw new IllegalArgumentException("Profile revision must be at least 1");
        }
        List<String> labUnitIds = normalizeLabUnits(request.getTestUnitIds());

        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName(name);
        analyzer.setTestUnitIds(labUnitIds);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        analyzer.setSysUserId(exactActor);
        profileBindingService.assignProfile(analyzer, profileId, profileRevision, exactActor);
        String analyzerId = analyzerService.insert(analyzer);
        if (analyzer.getId() == null) {
            analyzer.setId(analyzerId);
        }
        return state(analyzer, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerInstanceState> list() {
        List<Analyzer> analyzers = analyzerService.getAllWithBindings();
        List<String> analyzerIds = analyzers.stream().map(Analyzer::getId).filter(Objects::nonNull).toList();
        Map<String, Long> heldCounts = analyzerResultsService.countHeldResultsByAnalyzerIds(analyzerIds);
        return analyzers.stream().map(analyzer -> state(analyzer, heldCounts.getOrDefault(analyzer.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerInstanceState get(String analyzerId) {
        return state(find(analyzerId));
    }

    @Override
    @Transactional
    public AnalyzerInstanceState update(String analyzerId, AnalyzerInstanceRequest request, String actor) {
        if (request == null) {
            throw new IllegalArgumentException("Analyzer request is required");
        }
        Analyzer analyzer = find(analyzerId);
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        String requestedProfileId = requireText(request.getProfileId(), "Profile ID");
        int requestedRevision = request.getProfileRevision() == null ? 0 : request.getProfileRevision();
        if (profile == null || !requestedProfileId.equals(profile.getProfileId())
                || requestedRevision != profile.getProfileRevision()) {
            throw new IllegalArgumentException("A configured analyzer cannot be moved to another profile revision");
        }
        analyzer.setName(requireText(request.getName(), "Analyzer name"));
        analyzer.setTestUnitIds(normalizeLabUnits(request.getTestUnitIds()));
        analyzer.setSysUserId(requireText(actor, "actor"));
        analyzerService.update(analyzer);
        return state(analyzer);
    }

    @Override
    @Transactional
    public AnalyzerInstanceState selectSiteBindingRevision(String analyzerId, String siteBindingId, int revision,
            String bindingFingerprint, String actor) {
        Analyzer analyzer = find(analyzerId);
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        if (profile == null || profile.getId() == null) {
            throw new IllegalStateException("Analyzer profile binding is missing");
        }
        AnalyzerSiteBindingSnapshot current = siteBindingService.findCurrentByProfileBindingId(profile.getId())
                .orElseThrow(() -> new IllegalArgumentException("Analyzer Type mappings are missing"));
        if (!Objects.equals(requireText(siteBindingId, "Site binding ID"), current.binding().getId())
                || revision != current.revision().getRevisionNumber()
                || !Objects.equals(requireText(bindingFingerprint, "Binding fingerprint"),
                        current.revision().getBindingFingerprint())) {
            throw new IllegalArgumentException("Analyzer Type mappings changed after Verify was loaded");
        }
        if (analyzer.getSiteBindingRevision() != null
                && Objects.equals(analyzer.getSiteBindingRevision().getId(), current.revision().getId())) {
            return state(analyzer);
        }
        analyzer.setSiteBindingRevision(current.revision());
        analyzer.setSysUserId(requireText(actor, "actor"));
        analyzerService.update(analyzer);
        return state(analyzer);
    }

    @Override
    @Transactional
    public AnalyzerInstanceState attachBridgeConnection(String analyzerId, String bridgeConnectionId, String actor) {
        Analyzer analyzer = find(analyzerId);
        String exactConnectionId = requireText(bridgeConnectionId, "Bridge connection ID");
        if (analyzer.getBridgeConnectionId() != null && !exactConnectionId.equals(analyzer.getBridgeConnectionId())) {
            throw new IllegalStateException("Analyzer already references a different Bridge connection");
        }
        if (exactConnectionId.equals(analyzer.getBridgeConnectionId())) {
            return state(analyzer);
        }
        analyzer.setBridgeConnectionId(exactConnectionId);
        analyzer.setSysUserId(requireText(actor, "actor"));
        analyzerService.update(analyzer);
        return state(analyzer);
    }

    private Analyzer find(String analyzerId) {
        String exactId = requireText(analyzerId, "Analyzer ID");
        return analyzerService.getWithBinding(exactId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + exactId));
    }

    private AnalyzerInstanceState state(Analyzer analyzer) {
        long heldResultCount = analyzer.getId() == null ? 0L
                : analyzerResultsService.countHeldResultsByAnalyzerIds(List.of(analyzer.getId()))
                        .getOrDefault(analyzer.getId(), 0L);
        return state(analyzer, heldResultCount);
    }

    private static AnalyzerInstanceState state(Analyzer analyzer, long heldResultCount) {
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        if (profile == null) {
            throw new IllegalStateException("Analyzer profile binding is missing");
        }
        return new AnalyzerInstanceState(analyzer.getId(), analyzer.getName(), analyzer.getTestUnitIds(),
                profile.getProfileId(), profile.getProfileRevision(), profile.getProfileFingerprint(),
                analyzer.getBridgeConnectionId(), analyzer.getStatus(), heldResultCount);
    }

    private static List<String> normalizeLabUnits(List<String> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("At least one lab unit is required");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) {
                normalized.add(id.trim());
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one lab unit is required");
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
