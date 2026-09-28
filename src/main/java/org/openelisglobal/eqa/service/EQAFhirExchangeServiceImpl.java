package org.openelisglobal.eqa.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EQAFhirExchangeServiceImpl implements EQAFhirExchangeService {

    /**
     * System actor for exchange-initiated writes (admin user id, as the schedulers
     * use).
     */
    private static final String EXCHANGE_USER = "1";

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private ShippingBoxDAO shippingBoxDAO;
    @Autowired
    private EQAProviderScoringService scoringService;
    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;
    @Autowired
    private AnalyteService analyteService;

    @Override
    public int pollParticipantReports() {
        if (fhirConfig.getLocalFhirStorePath() == null || fhirConfig.getLocalFhirStorePath().isBlank()) {
            return 0;
        }
        IGenericClient client = fhirUtil.getLocalFhirClient();
        int applied = 0;
        for (DiagnosticReport report : reportsWithIdentifierSystem(client,
                fhirConfig.getOeFhirSystem() + EQAFhirSubmissionService.CONSIGNMENT_SUFFIX)) {
            // Score reports carry the consignment identifier too; they are this
            // provider's own writes, not inbound results.
            if (identifierValue(report, EQAFhirSubmissionService.SCORES_SUFFIX) != null) {
                continue;
            }
            try {
                if (applyParticipantReport(report, observationsOf(client, report))) {
                    applied++;
                }
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "pollParticipantReports",
                        "Participant report " + report.getIdElement().getIdPart() + " not applied: " + e.getMessage());
            }
        }
        return applied;
    }

    @Override
    public int pollScoreReports() {
        int applied = 0;
        for (String remoteStorePath : remoteStorePaths()) {
            IGenericClient client = fhirUtil.getFhirClient(remoteStorePath);
            for (DiagnosticReport report : reportsWithIdentifierSystem(client,
                    fhirConfig.getOeFhirSystem() + EQAFhirSubmissionService.SCORES_SUFFIX)) {
                try {
                    if (applyScoreReport(report, observationsOf(client, report))) {
                        applied++;
                    }
                } catch (RuntimeException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "pollScoreReports",
                            "Score report " + report.getIdElement().getIdPart() + " not applied: " + e.getMessage());
                }
            }
        }
        return applied;
    }

    @Override
    public boolean applyParticipantReport(DiagnosticReport report, List<Observation> observations) {
        ShippingBox box = boxFor(report);
        if (box == null || box.getEqaCycleId() == null || box.getDestinationFacility() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "applyParticipantReport",
                    "Participant report " + report.getIdElement().getIdPart()
                            + " names no consignment this instance dispatched for a cycle — not applied");
            return false;
        }
        Long organizationId = Long.valueOf(box.getDestinationFacility().getId());
        Map<String, String> byAnalyteName = new LinkedHashMap<>();
        for (Observation observation : observations) {
            String name = analyteNameOf(observation);
            String value = reportedValueOf(observation);
            if (name != null && value != null) {
                byAnalyteName.put(name, value);
            }
        }
        if (byAnalyteName.isEmpty()) {
            return false;
        }
        // The poll repeats: a report whose values are already on file is a read.
        if (alreadyOnFile(box.getEqaCycleId(), organizationId, byAnalyteName)) {
            return false;
        }
        Map<String, Object> grid = scoringService.takeInByAnalyteName(box.getEqaCycleId(), organizationId,
                byAnalyteName, EQASubmissionMethod.FHIR, EXCHANGE_USER);
        List<?> unmapped = (List<?>) grid.get("unmapped");
        if (!unmapped.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "applyParticipantReport", "Consignment " + box.getBoxId()
                    + ": no test in the scheme reports " + unmapped + " — those values were skipped");
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "applyParticipantReport", "Consignment " + box.getBoxId()
                + ": " + (byAnalyteName.size() - unmapped.size()) + " participant result(s) taken in from the store");
        return byAnalyteName.size() > unmapped.size();
    }

    @Override
    public boolean applyScoreReport(DiagnosticReport report, List<Observation> observations) {
        ShippingBox box = boxFor(report);
        if (box == null || box.getEqaCycleId() == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "applyScoreReport",
                    "Score report " + report.getIdElement().getIdPart()
                            + " names no consignment this laboratory imported — not applied");
            return false;
        }
        List<Map<String, Object>> scores = new ArrayList<>();
        for (Observation observation : observations) {
            String name = analyteNameOf(observation);
            String performance = performanceOf(observation);
            if (name == null || performance == null) {
                continue;
            }
            Analyte probe = new Analyte();
            probe.setAnalyteName(name);
            Analyte analyte = analyteService.getAnalyteByName(probe, true);
            if (analyte == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "applyScoreReport",
                        "Score for analyte '" + name + "' skipped: this laboratory has no analyte of that name");
                continue;
            }
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("analyteId", Long.valueOf(analyte.getId()));
            score.put("performance", performance);
            BigDecimal z = zScoreOf(observation);
            if (z != null) {
                score.put("zScore", z);
            }
            scores.add(score);
        }
        if (scores.isEmpty()) {
            return false;
        }
        try {
            int scored = cycleSubmissionService.intakeScores(box.getEqaCycleId(), null, scores, EXCHANGE_USER);
            LogEvent.logInfo(this.getClass().getSimpleName(), "applyScoreReport",
                    "Consignment " + box.getBoxId() + ": " + scored + " score(s) taken in from the provider's store");
            return scored > 0;
        } catch (IllegalStateException alreadyScored) {
            // Replay of a report the lab has already taken in — the intake's 409 case.
            return false;
        } catch (IllegalArgumentException noMatch) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "applyScoreReport",
                    "Consignment " + box.getBoxId() + ": scores not applied — " + noMatch.getMessage());
            return false;
        }
    }

    // ---- helpers ----

    private ShippingBox boxFor(DiagnosticReport report) {
        String consignment = identifierValue(report, EQAFhirSubmissionService.CONSIGNMENT_SUFFIX);
        if (consignment == null) {
            return null;
        }
        try {
            return shippingBoxDAO.findByFhirUuid(UUID.fromString(consignment));
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    private String identifierValue(DiagnosticReport report, String systemSuffix) {
        String system = fhirConfig.getOeFhirSystem() + systemSuffix;
        for (Identifier identifier : report.getIdentifier()) {
            if (system.equals(identifier.getSystem()) && identifier.hasValue()) {
                return identifier.getValue();
            }
        }
        return null;
    }

    private boolean alreadyOnFile(Long cycleId, Long organizationId, Map<String, String> byAnalyteName) {
        Map<String, Object> grid = scoringService.intakeGrid(cycleId, organizationId);
        Map<String, String> onFile = new LinkedHashMap<>();
        for (Object row : (List<?>) grid.get("tests")) {
            Map<?, ?> test = (Map<?, ?>) row;
            if (test.get("analyteName") != null && test.get("reported") != null) {
                onFile.put(String.valueOf(test.get("analyteName")).trim().toLowerCase(),
                        normalise(String.valueOf(test.get("reported"))));
            }
        }
        for (Map.Entry<String, String> entry : byAnalyteName.entrySet()) {
            String current = onFile.get(entry.getKey().trim().toLowerCase());
            if (current == null || !current.equals(normalise(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private static String normalise(String value) {
        try {
            return new BigDecimal(value.trim()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return value.trim().toLowerCase();
        }
    }

    private static String analyteNameOf(Observation observation) {
        if (observation.getCode() == null) {
            return null;
        }
        if (observation.getCode().hasText()) {
            return observation.getCode().getText();
        }
        for (Coding coding : observation.getCode().getCoding()) {
            if (coding.hasDisplay()) {
                return coding.getDisplay();
            }
        }
        return null;
    }

    private static String reportedValueOf(Observation observation) {
        if (observation.hasValueQuantity() && observation.getValueQuantity().hasValue()) {
            return observation.getValueQuantity().getValue().toPlainString();
        }
        if (observation.hasValueStringType()) {
            return ((StringType) observation.getValue()).getValue();
        }
        return null;
    }

    private String performanceOf(Observation observation) {
        String system = fhirConfig.getOeFhirSystem() + "/eqa/performance";
        for (CodeableConcept concept : observation.getInterpretation()) {
            for (Coding coding : concept.getCoding()) {
                if (system.equals(coding.getSystem()) && coding.hasCode()) {
                    return coding.getCode();
                }
            }
        }
        return null;
    }

    private static BigDecimal zScoreOf(Observation observation) {
        for (Observation.ObservationComponentComponent component : observation.getComponent()) {
            for (Coding coding : component.getCode().getCoding()) {
                if ("z-score".equals(coding.getCode()) && component.hasValueQuantity()) {
                    Quantity quantity = component.getValueQuantity();
                    return quantity.hasValue() ? quantity.getValue() : null;
                }
            }
        }
        return null;
    }

    private List<String> remoteStorePaths() {
        List<String> remotes = new ArrayList<>();
        if (fhirConfig.getRemoteStorePaths() == null) {
            return remotes;
        }
        for (String path : fhirConfig.getRemoteStorePaths()) {
            if (path != null && !path.isBlank() && !path.equals(fhirConfig.getLocalFhirStorePath())) {
                remotes.add(path);
            }
        }
        return remotes;
    }

    private static List<DiagnosticReport> reportsWithIdentifierSystem(IGenericClient client, String system) {
        List<DiagnosticReport> reports = new ArrayList<>();
        Bundle bundle = client.search().forResource(DiagnosticReport.class)
                .where(DiagnosticReport.IDENTIFIER.hasSystemWithAnyCode(system)).returnBundle(Bundle.class).execute();
        collect(bundle, reports);
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            collect(bundle, reports);
        }
        return reports;
    }

    private static void collect(Bundle bundle, List<DiagnosticReport> target) {
        if (bundle == null || !bundle.hasEntry()) {
            return;
        }
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource() && entry.getResource() instanceof DiagnosticReport report) {
                target.add(report);
            }
        }
    }

    private static List<Observation> observationsOf(IGenericClient client, DiagnosticReport report) {
        List<Observation> observations = new ArrayList<>();
        for (Reference reference : report.getResult()) {
            if (!reference.hasReference()) {
                continue;
            }
            String id = reference.getReferenceElement().getIdPart();
            try {
                observations.add(client.read().resource(Observation.class).withId(id).execute());
            } catch (RuntimeException e) {
                LogEvent.logWarn(EQAFhirExchangeServiceImpl.class.getSimpleName(), "observationsOf",
                        "Observation " + id + " referenced by report " + report.getIdElement().getIdPart()
                                + " could not be read: " + e.getMessage());
            }
        }
        return observations;
    }
}
