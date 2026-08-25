package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.form.AnalyzerMigrationReferenceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Temporary atomic writer for the one-time analyzer connection cutover. */
@Service
public class AnalyzerMigrationReferenceService {

    private final AnalyzerService analyzerService;
    private final AnalyzerProfileBindingService profileBindingService;

    public AnalyzerMigrationReferenceService(AnalyzerService analyzerService,
            AnalyzerProfileBindingService profileBindingService) {
        this.analyzerService = analyzerService;
        this.profileBindingService = profileBindingService;
    }

    @Transactional
    public AnalyzerMigrationReferenceView attach(String analyzerId, AnalyzerMigrationReferenceRequest request,
            String actor) {
        if (request == null) {
            throw new IllegalArgumentException("Migration reference request is required");
        }
        Analyzer analyzer = find(analyzerId);
        String connectionId = requireText(request.getBridgeConnectionId(), "Bridge connection ID");
        if (analyzer.getBridgeConnectionId() != null && !connectionId.equals(analyzer.getBridgeConnectionId())) {
            throw new IllegalStateException("Analyzer already references a different Bridge connection");
        }

        AnalyzerProfileBinding existing = analyzer.getPinnedProfileBinding();
        if (connectionId.equals(analyzer.getBridgeConnectionId()) && existing != null) {
            requireExactProfile(existing, request);
            return view(analyzer, existing);
        }

        String exactActor = requireText(actor, "actor");
        AnalyzerProfileBinding selected = profileBindingService.assignProfile(analyzer,
                requireText(request.getProfileId(), "profile ID"), request.getProfileRevision(), exactActor);
        requireExactProfile(selected, request);
        analyzer.setBridgeConnectionId(connectionId);
        analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
        analyzer.setActive(false);
        analyzer.setSysUserId(exactActor);
        analyzer.setLastupdatedFields();
        analyzerService.update(analyzer);
        return view(analyzer, selected);
    }

    @Transactional(readOnly = true)
    public AnalyzerMigrationReferenceView get(String analyzerId) {
        Analyzer analyzer = find(analyzerId);
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        if (profile == null || analyzer.getBridgeConnectionId() == null) {
            throw new IllegalStateException("Analyzer migration reference is incomplete");
        }
        return view(analyzer, profile);
    }

    private Analyzer find(String analyzerId) {
        String exactId = requireText(analyzerId, "analyzer ID");
        return analyzerService.getWithType(exactId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + exactId));
    }

    private static void requireExactProfile(AnalyzerProfileBinding selected,
            AnalyzerMigrationReferenceRequest request) {
        if (selected == null || !requireText(request.getProfileId(), "profile ID").equals(selected.getProfileId())
                || request.getProfileRevision() != selected.getProfileRevision()
                || !requireText(request.getProfileFingerprint(), "profile fingerprint")
                        .equals(selected.getProfileFingerprint())) {
            throw new IllegalStateException("Selected profile evidence does not match the Bridge catalog");
        }
    }

    private static AnalyzerMigrationReferenceView view(Analyzer analyzer, AnalyzerProfileBinding profile) {
        return new AnalyzerMigrationReferenceView(analyzer.getId(), analyzer.getBridgeConnectionId(),
                new AnalyzerMigrationReferenceView.ProfileReference(profile.getProfileId(),
                        profile.getProfileRevision(), profile.getProfileFingerprint()));
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
