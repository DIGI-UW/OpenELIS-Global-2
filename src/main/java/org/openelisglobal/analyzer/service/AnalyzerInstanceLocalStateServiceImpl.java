package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerInstanceLocalStateServiceImpl implements AnalyzerInstanceLocalStateService {

    private final AnalyzerService analyzerService;
    private final AnalyzerMappingService mappingService;
    private final AnalyzerMappingEditorService mappingEditorService;
    private final AnalyzerResultsService analyzerResultsService;
    private final AnalyzerNormalizedResultImportService importService;

    @Autowired
    public AnalyzerInstanceLocalStateServiceImpl(AnalyzerService analyzerService, AnalyzerMappingService mappingService,
            AnalyzerMappingEditorService mappingEditorService, AnalyzerResultsService analyzerResultsService,
            AnalyzerNormalizedResultImportService importService) {
        this.analyzerService = analyzerService;
        this.mappingService = mappingService;
        this.mappingEditorService = mappingEditorService;
        this.analyzerResultsService = analyzerResultsService;
        this.importService = importService;
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
        String analyzerId = analyzerService.insert(analyzer);
        if (analyzer.getId() == null) {
            analyzer.setId(analyzerId);
        }
        Analyzer withMapping = copyForUpdate(analyzer);
        withMapping
                .setMapping(mappingService.assignProfile(analyzer, profileId, profileRevision, exactActor).mapping());
        analyzerService.update(withMapping);
        return state(withMapping, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerInstanceState> list() {
        List<Analyzer> analyzers = analyzerService.getAllWithMapping();
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
        Analyzer analyzer = copyForUpdate(find(analyzerId));
        AnalyzerProfilePin profile = analyzer.getPinnedProfile();
        String requestedProfileId = requireText(request.getProfileId(), "Profile ID");
        int requestedRevision = request.getProfileRevision() == null ? 0 : request.getProfileRevision();
        String exactActor = requireText(actor, "actor");
        String name = requireText(request.getName(), "Analyzer name");
        List<String> labUnitIds = normalizeLabUnits(request.getTestUnitIds());
        if (requestedRevision < 1) {
            throw new IllegalArgumentException("Profile revision must be at least 1");
        }
        if (profile == null && isUnconfiguredDraft(analyzer)) {
            analyzer.setMapping(mappingService
                    .assignProfile(analyzer, requestedProfileId, requestedRevision, exactActor).mapping());
            analyzer.setBridgeConnectionId(null);
            analyzer.setActive(false);
        } else if (profile == null && isInactiveWithoutMapping(analyzer)) {
            analyzer.setMapping(mappingService
                    .assignProfile(analyzer, requestedProfileId, requestedRevision, exactActor).mapping());
        } else if (profile == null || !requestedProfileId.equals(profile.getProfileId())) {
            throw new IllegalArgumentException("A configured analyzer keeps its profile");
        } else if (requestedRevision != profile.getProfileRevision()) {
            throw new IllegalArgumentException("Adopt revision " + requestedRevision + " of " + requestedProfileId
                    + " to move this analyzer to it");
        }
        analyzer.setName(name);
        analyzer.setTestUnitIds(labUnitIds);
        analyzer.setSysUserId(exactActor);
        analyzerService.update(analyzer);
        return state(analyzer);
    }

    @Override
    @Transactional
    public AnalyzerInstanceState applyMapping(String analyzerId, String mappingId, int revision,
            String mappingFingerprint, String actor) {
        Analyzer analyzer = find(analyzerId);
        if (analyzer.getBridgeConnectionId() != null) {
            analyzer = analyzerService.findByBridgeConnectionIdForUpdate(analyzer.getBridgeConnectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Analyzer connection is missing"));
        }
        AnalyzerMappingSnapshot current = mappingService.findLatestByAnalyzerId(analyzer.getId())
                .orElseThrow(() -> new IllegalArgumentException("The analyzer has no mapping"));
        if (!Objects.equals(requireText(mappingId, "Mapping ID"), current.mapping().getId())
                || revision != current.mapping().getRevisionNumber()
                || !Objects.equals(requireText(mappingFingerprint, "Mapping fingerprint"),
                        current.mapping().getMappingFingerprint())) {
            throw new IllegalArgumentException("The analyzer's mapping changed after Verify was loaded");
        }
        AnalyzerMappingView mapping = mappingEditorService.getMapping(analyzer.getId());
        if (mapping.confirmation().state() != AnalyzerMappingConfirmationView.State.CURRENT
                || !Objects.equals(mapping.mappingId(), current.mapping().getId())
                || mapping.mappingRevision() != revision
                || !Objects.equals(mapping.mappingFingerprint(), current.mapping().getMappingFingerprint())) {
            throw new IllegalArgumentException("Confirm the analyzer's current mapping before applying it");
        }
        if (analyzer.getMapping() != null && Objects.equals(analyzer.getMapping().getId(), current.mapping().getId())) {
            importService.recoverHeldMappingResults(analyzer.getId(), actor);
            return state(analyzer);
        }
        analyzer = copyForUpdate(analyzer);
        analyzer.setMapping(current.mapping());
        analyzer.setSysUserId(requireText(actor, "actor"));
        analyzerService.update(analyzer);
        importService.recoverHeldMappingResults(analyzer.getId(), actor);
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
        analyzer = copyForUpdate(analyzer);
        analyzer.setBridgeConnectionId(exactConnectionId);
        analyzer.setSysUserId(requireText(actor, "actor"));
        analyzerService.update(analyzer);
        return state(analyzer);
    }

    private static Analyzer copyForUpdate(Analyzer persisted) {
        // The audited service compares the update with the managed database object.
        // Mutating that object first erases the previous state and suppresses history.
        Analyzer update = new Analyzer();
        BeanUtils.copyProperties(persisted, update);
        update.setTestUnitIds(new ArrayList<>(persisted.getTestUnitIds()));
        return update;
    }

    private Analyzer find(String analyzerId) {
        String exactId = requireText(analyzerId, "Analyzer ID");
        return analyzerService.getWithMapping(exactId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + exactId));
    }

    private AnalyzerInstanceState state(Analyzer analyzer) {
        long heldResultCount = analyzer.getId() == null ? 0L
                : analyzerResultsService.countHeldResultsByAnalyzerIds(List.of(analyzer.getId()))
                        .getOrDefault(analyzer.getId(), 0L);
        return state(analyzer, heldResultCount);
    }

    private static AnalyzerInstanceState state(Analyzer analyzer, long heldResultCount) {
        AnalyzerProfilePin profile = analyzer.getPinnedProfile();
        if (profile == null) {
            // Preserved pre-Bridge records must remain visible while migration is pending.
            return new AnalyzerInstanceState(analyzer.getId(), analyzer.getName(), analyzer.getTestUnitIds(), "", 0, "",
                    null, analyzer.getStatus(), heldResultCount);
        }
        return new AnalyzerInstanceState(analyzer.getId(), analyzer.getName(), analyzer.getTestUnitIds(),
                profile.getProfileId(), profile.getProfileRevision(), profile.getProfileFingerprint(),
                analyzer.getBridgeConnectionId(), analyzer.getStatus(), heldResultCount);
    }

    private static boolean isUnconfiguredDraft(Analyzer analyzer) {
        return analyzer.getStatus() == Analyzer.AnalyzerStatus.SETUP
                && (analyzer.getBridgeConnectionId() == null || analyzer.getBridgeConnectionId().isBlank())
                && analyzer.getMapping() == null && analyzer.getLastActivatedDate() == null
                && analyzer.getLatestActivationRecord() == null;
    }

    // Changeset 124 leaves every analyzer that had a shared mapping this way. It
    // keeps its Bridge connection and stays inactive until verified and activated.
    private static boolean isInactiveWithoutMapping(Analyzer analyzer) {
        return analyzer.getStatus() == Analyzer.AnalyzerStatus.INACTIVE && analyzer.getMapping() == null;
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
