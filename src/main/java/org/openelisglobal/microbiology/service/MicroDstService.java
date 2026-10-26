package org.openelisglobal.microbiology.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroDstService {
    private final MicroIsolateDAO isolates;
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;
    private final MicroAstRunDAO runs;
    private final MicroAstReportChoiceService choices;
    private final MicroCaseAnalysisDAO links;
    private final MicroMolecularDstBindingDAO bindings;
    private final MicroDstResolutionDAO resolutions;
    private final ResultService results;
    private final IStatusService statuses;
    private final MicroCaseAuthorizationService authorization;

    public MicroDstService(MicroIsolateDAO isolates, MicroOrganismDAO organisms, MicroAntibioticDAO antibiotics,
            MicroAstRunDAO runs, MicroAstReportChoiceService choices, MicroCaseAnalysisDAO links,
            MicroMolecularDstBindingDAO bindings, MicroDstResolutionDAO resolutions, ResultService results,
            IStatusService statuses, MicroCaseAuthorizationService authorization) {
        this.isolates = isolates;
        this.organisms = organisms;
        this.antibiotics = antibiotics;
        this.runs = runs;
        this.choices = choices;
        this.links = links;
        this.bindings = bindings;
        this.resolutions = resolutions;
        this.results = results;
        this.statuses = statuses;
        this.authorization = authorization;
    }

    public MicroDstForm evaluate(String caseId) {
        var form = new MicroDstForm();
        var identified = isolates.getByCaseId(caseId).stream().filter(i -> i.getCancelledAt() == null
                && "CONFIRMED".equals(i.getIdentificationStatus()) && i.getOrganismId() != null).toList();
        var tb = identified.stream()
                .filter(i -> "TB".equals(organisms.get(i.getOrganismId()).orElseThrow().getSusceptibilityProtocol()))
                .toList();
        if (tb.isEmpty() && identified.stream().anyMatch(
                i -> "NTM".equals(organisms.get(i.getOrganismId()).orElseThrow().getSusceptibilityProtocol()))) {
            form.classification = "NTM";
            return form;
        }
        Map<String, List<MicroDstForm.Evidence>> evidence = new TreeMap<>();
        for (var i : tb)
            for (var run : runs.getByIsolateId(i.getId())) {
                if (List.of("INVALIDATED", "RERUN_REQUIRED", "CANCELLED").contains(run.getStatus()))
                    continue;
                for (var r : choices.latest(run.getId()))
                    evidence.computeIfAbsent(r.getAntibioticId(), x -> new ArrayList<>())
                            .add(new MicroDstForm.Evidence("PHENOTYPIC", r.getId(),
                                    MicroAstReportChoiceService.interpretation(r), "REVIEWED".equals(run.getStatus())));
            }
        var configured = bindings.getBindings();
        for (var a : links.getAnalyses(caseId)) {
            var link = links.getByCaseAndAnalysis(caseId, a.getId());
            if (!"INITIAL".equals(link.getPlacement()) || link.getCancelledAt() != null)
                continue;
            for (var result : results.getResultsByAnalysis(a)) {
                if (result.getTestResult() == null || result.getTestResult().getComponentId() == null)
                    continue;
                for (var b : configured)
                    if (b.getTestId().equals(a.getTest().getId())
                            && b.getComponentId().equals(result.getTestResult().getComponentId())) {
                        String interpretation = Objects.equals(b.getResistantValue(), result.getValue()) ? "RESISTANT"
                                : Objects.equals(b.getSusceptibleValue(), result.getValue()) ? "SUSCEPTIBLE"
                                        : "INDETERMINATE";
                        evidence.computeIfAbsent(b.getAntibioticId(), x -> new ArrayList<>())
                                .add(new MicroDstForm.Evidence("MOLECULAR", result.getId(), interpretation,
                                        "Finalized".equals(statuses.getAnalysisStatusForID(a.getStatusId()).name())));
                    }
            }
        }
        if (tb.isEmpty() && evidence.isEmpty()) {
            form.classification = "NOT_APPLICABLE";
            return form;
        }
        var prior = resolutions.getByCaseId(caseId);
        var drugs = new ArrayList<MicroDstForm.Drug>();
        var classification = new ArrayList<MicroTbResistanceClassifier.Drug>();
        var relevantDrugs = new LinkedHashMap<String, MicroAntibiotic>();
        antibiotics.getActiveAntibiotics().stream().filter(a -> !"NONE".equals(a.getResistanceRole()))
                .forEach(a -> relevantDrugs.put(a.getId(), a));
        for (String drugId : evidence.keySet())
            relevantDrugs.computeIfAbsent(drugId, id -> antibiotics.get(id).orElseThrow());
        for (var antibiotic : relevantDrugs.values()) {
            var role = MicroTbResistanceClassifier.Role.valueOf(antibiotic.getResistanceRole());
            var entries = evidence
                    .getOrDefault(antibiotic.getId(), List.of()).stream().sorted(Comparator
                            .comparing(MicroDstForm.Evidence::source).thenComparing(MicroDstForm.Evidence::sourceId))
                    .toList();
            var observed = entries.stream().map(MicroDstForm.Evidence::interpretation)
                    .filter(v -> Set.of("SUSCEPTIBLE", "RESISTANT").contains(v))
                    .collect(java.util.stream.Collectors.toSet());
            boolean discordant = observed.size() > 1;
            String fingerprint = fingerprint(entries);
            var resolution = prior.stream()
                    .filter(r -> r.getAntibioticId().equals(antibiotic.getId())
                            && r.getEvidenceFingerprint().equals(fingerprint))
                    .max(Comparator.comparing(MicroDstResolution::getResolvedAt)).orElse(null);
            String interpretation = discordant ? resolution == null ? "DISCORDANT" : resolution.getInterpretation()
                    : observed.size() == 1 && entries.stream().allMatch(
                            e -> e.validated() && Set.of("SUSCEPTIBLE", "RESISTANT").contains(e.interpretation()))
                                    ? observed.iterator().next()
                                    : "INDETERMINATE";
            form.unresolvedDiscordance |= discordant && resolution == null;
            drugs.add(new MicroDstForm.Drug(antibiotic.getId(), antibiotic.getDisplayName(), role.name(),
                    interpretation, discordant, fingerprint, resolution == null ? null : resolution.getReason(),
                    resolution == null ? null : resolution.getResolvedBy(),
                    resolution == null ? null : resolution.getResolvedAt(), entries));
            classification.add(new MicroTbResistanceClassifier.Drug(antibiotic.getId(), role, interpretation));
        }
        form.drugs = drugs;
        form.classification = MicroTbResistanceClassifier.classify(classification).name();
        return form;
    }

    @Transactional
    public MicroDstForm resolve(String caseId, MicroDstForm.ResolutionRequest request, String actor) {
        authorization.requireWrite(caseId, actor, Constants.ROLE_VALIDATION);
        if (request == null || request.interpretation == null)
            throw new IllegalArgumentException("DST_RESOLUTION_INVALID");
        MicroCaseServiceImpl.requireText(request.reason, "reason");
        if (request.reason.length() > 4000 || !Set.of("SUSCEPTIBLE", "RESISTANT").contains(request.interpretation))
            throw new IllegalArgumentException("DST_RESOLUTION_INVALID");
        var drug = evaluate(caseId).drugs.stream().filter(d -> d.antibioticId().equals(request.antibioticId))
                .findFirst().orElseThrow();
        if (!drug.discordant() || !drug.evidenceFingerprint().equals(request.evidenceFingerprint))
            throw new MicroAstConflictException("DST_EVIDENCE_CHANGED");
        if (drug.evidence().stream().anyMatch(e -> !e.validated()))
            throw new MicroAstConflictException("DST_EVIDENCE_NOT_VALIDATED");
        var resolution = new MicroDstResolution();
        resolution.setCaseId(caseId);
        resolution.setAntibioticId(request.antibioticId);
        resolution.setEvidenceFingerprint(drug.evidenceFingerprint());
        resolution.setInterpretation(request.interpretation);
        resolution.setReason(request.reason.trim());
        resolution.setResolvedBy(actor);
        resolution.setResolvedAt(MicroCaseServiceImpl.now());
        resolutions.insert(resolution);
        return evaluate(caseId);
    }

    private static String fingerprint(List<MicroDstForm.Evidence> evidence) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(evidence.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
