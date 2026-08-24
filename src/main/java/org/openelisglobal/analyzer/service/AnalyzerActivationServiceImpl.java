package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerActivationServiceImpl implements AnalyzerActivationService {

    private static final String PROFILE_BLOCKER = "analyzer.activation.blocker.profile";
    private static final String NAME_BLOCKER = "analyzer.activation.blocker.name";
    private static final String LAB_UNIT_BLOCKER = "analyzer.activation.blocker.labUnit";
    private static final String TRANSPORT_BLOCKER = "analyzer.activation.blocker.transport";
    private static final String DATA_FLOW_BLOCKER = "analyzer.activation.blocker.dataFlow";
    private static final String MAPPINGS_BLOCKER = "analyzer.activation.blocker.mappings";
    private static final String RECOGNITION_BLOCKER = "analyzer.activation.blocker.recognition";
    private static final String CONNECTION_BLOCKER = "analyzer.activation.blocker.connection";
    private static final String BRIDGE_ACKNOWLEDGEMENT_BLOCKER = "analyzer.activation.blocker.bridgeAcknowledgement";

    private final AnalyzerService analyzerService;
    private final BridgeProfileCatalogService profileCatalogService;
    private final AnalyzerSiteBindingService siteBindingService;
    private final AnalyzerSiteBindingConfirmationService confirmationService;
    private final TestSectionService testSectionService;
    private final BridgeRegistrationService registrationService;
    private final AnalyzerActivationCandidateFactory candidateFactory;
    private final AnalyzerActivationCandidateService candidateService;
    private final Clock clock;

    @Autowired
    public AnalyzerActivationServiceImpl(AnalyzerService analyzerService,
            BridgeProfileCatalogService profileCatalogService, AnalyzerSiteBindingService siteBindingService,
            AnalyzerSiteBindingConfirmationService confirmationService, TestSectionService testSectionService,
            BridgeRegistrationService registrationService, AnalyzerActivationCandidateFactory candidateFactory,
            AnalyzerActivationCandidateService candidateService) {
        this(analyzerService, profileCatalogService, siteBindingService, confirmationService, testSectionService,
                registrationService, candidateFactory, candidateService, Clock.systemUTC());
    }

    AnalyzerActivationServiceImpl(AnalyzerService analyzerService, BridgeProfileCatalogService profileCatalogService,
            AnalyzerSiteBindingService siteBindingService, AnalyzerSiteBindingConfirmationService confirmationService,
            TestSectionService testSectionService, BridgeRegistrationService registrationService,
            AnalyzerActivationCandidateFactory candidateFactory, AnalyzerActivationCandidateService candidateService,
            Clock clock) {
        this.analyzerService = analyzerService;
        this.profileCatalogService = profileCatalogService;
        this.siteBindingService = siteBindingService;
        this.confirmationService = confirmationService;
        this.testSectionService = testSectionService;
        this.registrationService = registrationService;
        this.candidateFactory = candidateFactory;
        this.candidateService = candidateService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerActivationResult readiness(String analyzerId) {
        Analyzer analyzer = findAnalyzer(analyzerId);
        ActivationContext context = validateLocalCandidate(analyzer);
        if (!context.blockers().isEmpty()) {
            return AnalyzerActivationResult.blocked(analyzer, context.blockers());
        }
        try {
            registrationService.buildActivationRegistration(analyzer);
            return AnalyzerActivationResult.ready(analyzer);
        } catch (BridgeRegistrationException exception) {
            return AnalyzerActivationResult.blocked(analyzer, List.of(new AnalyzerActivationBlocker(CONNECTION_BLOCKER,
                    Map.of("detail", String.valueOf(exception.getMessage())))));
        }
    }

    @Override
    @Transactional
    public AnalyzerActivationResult activate(String analyzerId, String actor) {
        Analyzer analyzer = findAnalyzer(analyzerId);
        String effectiveAnalyzerId = analyzer.getId();
        String effectiveActor = requireText(actor, "actor");

        ActivationContext context = validateLocalCandidate(analyzer);
        if (!context.blockers().isEmpty()) {
            return AnalyzerActivationResult.blocked(analyzer, context.blockers());
        }

        ObjectNode registration;
        try {
            registration = registrationService.buildActivationRegistration(analyzer);
        } catch (BridgeRegistrationException exception) {
            return AnalyzerActivationResult.blocked(analyzer, List.of(new AnalyzerActivationBlocker(CONNECTION_BLOCKER,
                    Map.of("detail", String.valueOf(exception.getMessage())))));
        }

        BridgeRegistrationResult synchronization = registrationService.synchronizeCandidate(effectiveAnalyzerId,
                registration);
        Optional<BridgeRegisteredCandidate> acknowledgement = synchronization.candidate(effectiveAnalyzerId);
        if (acknowledgement.isEmpty()) {
            return AnalyzerActivationResult.blocked(analyzer,
                    List.of(new AnalyzerActivationBlocker(BRIDGE_ACKNOWLEDGEMENT_BLOCKER,
                            Map.of("detail", synchronization.failure() == null ? "" : synchronization.failure()))));
        }

        PreviousAnalyzerState previousState = PreviousAnalyzerState.capture(analyzer);
        try {
            AnalyzerActivationDocuments documents = candidateFactory.create(analyzer, context.snapshot(),
                    context.confirmation(), registration, acknowledgement.get());
            AnalyzerActivationCandidate retained = candidateService.retain(analyzer, context.snapshot().revision(),
                    context.confirmation(), documents, effectiveActor);
            analyzer.setActiveCandidate(retained);
            analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
            analyzer.setActive(true);
            analyzer.setLastActivatedDate(Date.from(clock.instant()));
            analyzer.setSysUserId(effectiveActor);
            analyzer.setLastupdatedFields();
            analyzerService.update(analyzer);
            return AnalyzerActivationResult.activated(analyzer);
        } catch (RuntimeException exception) {
            previousState.restore(analyzer);
            try {
                registrationService.synchronize();
            } catch (RuntimeException compensationFailure) {
                exception.addSuppressed(compensationFailure);
            }
            throw exception;
        }
    }

    private Analyzer findAnalyzer(String analyzerId) {
        String effectiveAnalyzerId = requireText(analyzerId, "analyzer ID");
        return analyzerService.getWithType(effectiveAnalyzerId)
                .orElseThrow(() -> new IllegalArgumentException("Analyzer not found: " + effectiveAnalyzerId));
    }

    private ActivationContext validateLocalCandidate(Analyzer analyzer) {
        List<AnalyzerActivationBlocker> blockers = new ArrayList<>();
        if (analyzer.getName() == null || analyzer.getName().isBlank()) {
            blockers.add(new AnalyzerActivationBlocker(NAME_BLOCKER));
        }
        if (analyzer.getTestUnitIds() == null || analyzer.getTestUnitIds().stream().noneMatch(this::isActiveLabUnit)) {
            blockers.add(new AnalyzerActivationBlocker(LAB_UNIT_BLOCKER));
        }

        AnalyzerProfileBinding profileBinding = analyzer.getPinnedProfileBinding();
        BridgeProfileCatalog.ProfileRevision profileRevision = null;
        if (profileBinding == null) {
            blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
        } else {
            try {
                profileRevision = profileCatalogService.getProfile(profileBinding.getProfileId(),
                        profileBinding.getProfileRevision());
                BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(profileRevision.profile());
                if (!"ACTIVE".equals(profile.status())
                        || !Objects.equals(profileBinding.getProfileFingerprint(), profile.revisionFingerprint())
                        || analyzer.getType() == null
                        || !profile.protocol().equalsIgnoreCase(analyzer.getType().trim())) {
                    blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
                } else {
                    if (!profile.declaresTransport(analyzer.getTransportMode())) {
                        blockers.add(new AnalyzerActivationBlocker(TRANSPORT_BLOCKER));
                    }
                    if (!profile.supportsDataFlow(analyzer.getCommunicationMode())) {
                        blockers.add(new AnalyzerActivationBlocker(DATA_FLOW_BLOCKER));
                    }
                }
            } catch (BridgeProfileCatalogException | IllegalArgumentException exception) {
                blockers.add(new AnalyzerActivationBlocker(PROFILE_BLOCKER));
            }
        }

        AnalyzerSiteBindingSnapshot snapshot = null;
        AnalyzerSiteBindingConfirmation confirmation = null;
        String siteBindingRevisionId = analyzer.getSiteBindingRevision() == null ? null
                : analyzer.getSiteBindingRevision().getId();
        String recognitionFingerprint = profileRevision == null || profileRevision.controlRecognitionSummary() == null
                ? null
                : profileRevision.controlRecognitionSummary().recognitionFingerprint();
        if (siteBindingRevisionId != null && recognitionFingerprint != null) {
            snapshot = siteBindingService.findByRevisionId(siteBindingRevisionId).orElse(null);
            if (snapshot != null) {
                AnalyzerSiteBindingVerificationAssessment assessment = confirmationService.assessCurrent(snapshot,
                        recognitionFingerprint);
                if (!assessment.mappingsCurrent()) {
                    blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
                }
                if (!assessment.recognitionCurrent()) {
                    blockers.add(new AnalyzerActivationBlocker(RECOGNITION_BLOCKER));
                }
                confirmation = assessment.currentConfirmation().orElse(null);
            }
        }
        if (snapshot == null) {
            blockers.add(new AnalyzerActivationBlocker(MAPPINGS_BLOCKER));
            blockers.add(new AnalyzerActivationBlocker(RECOGNITION_BLOCKER));
        }
        return new ActivationContext(snapshot, confirmation, List.copyOf(blockers));
    }

    private boolean isActiveLabUnit(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        var testSection = testSectionService.get(id.trim());
        return testSection != null && "Y".equalsIgnoreCase(testSection.getIsActive());
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record ActivationContext(AnalyzerSiteBindingSnapshot snapshot, AnalyzerSiteBindingConfirmation confirmation,
            List<AnalyzerActivationBlocker> blockers) {
    }

    private record PreviousAnalyzerState(Analyzer.AnalyzerStatus status, boolean active,
            AnalyzerActivationCandidate activeCandidate, Date lastActivatedDate, String sysUserId,
            Timestamp lastupdated) {

        static PreviousAnalyzerState capture(Analyzer analyzer) {
            return new PreviousAnalyzerState(analyzer.getStatus(), analyzer.isActive(), analyzer.getActiveCandidate(),
                    analyzer.getLastActivatedDate(), analyzer.getSysUserId(), analyzer.getLastupdated());
        }

        void restore(Analyzer analyzer) {
            analyzer.setStatus(status);
            analyzer.setActive(active);
            analyzer.setActiveCandidate(activeCandidate);
            analyzer.setLastActivatedDate(lastActivatedDate);
            analyzer.setSysUserId(sysUserId);
            analyzer.setLastupdated(lastupdated);
        }
    }
}
