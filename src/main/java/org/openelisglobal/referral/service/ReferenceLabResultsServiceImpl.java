package org.openelisglobal.referral.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.dto.ReferenceLabReferralDTO;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceLabResultsServiceImpl implements ReferenceLabResultsService {

    private static final List<ReferralStatus> OUTSTANDING_STATUSES = Arrays.asList(ReferralStatus.REQUESTED,
            ReferralStatus.RECEIVED, ReferralStatus.IN_PROGRESS);
    private static final List<ReferralStatus> RETURNED_STATUSES = Collections.singletonList(ReferralStatus.COMPLETED);
    // History bucket fetches COMPLETED too — they're split from Returned in-memory
    // by the manually_entered flag (manual entries skip Returned per OGC-799 AC).
    private static final List<ReferralStatus> HISTORY_STATUSES = Arrays.asList(ReferralStatus.REJECTED,
            ReferralStatus.CANCELLED, ReferralStatus.COMPLETED);

    private static final String STUCK_THRESHOLD_CONFIG = "referralStuckThresholdDays";
    private static final int STUCK_THRESHOLD_DEFAULT_DAYS = 7;

    @Autowired
    private ReferralDAO referralDAO;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private FhirApiWorkflowService fhirApiWorkflowService;

    @Autowired
    private org.openelisglobal.referral.fhir.service.FhirReferralService fhirReferralService;

    @Autowired
    private ReferralService referralService;

    @Override
    public void acceptReferral(String referralId, String actorUserId) {
        Referral referral = referralDAO.getReferralById(referralId);
        if (referral == null) {
            throw new IllegalArgumentException("Referral not found: " + referralId);
        }
        // Only a returned (COMPLETED) result that hasn't been accepted yet can be
        // accepted; anything else is a no-op so repeat clicks don't double-post.
        if (referral.getStatus() != ReferralStatus.COMPLETED || Boolean.TRUE.equals(referral.getReconciled())) {
            return;
        }
        UUID referralTaskUuid = referral.getFhirUuid();
        if (referralTaskUuid == null) {
            throw new IllegalStateException("Referral " + referralId + " has no FHIR uuid to fetch results for");
        }
        List<ReferralResultsImportObjects> imports = fhirApiWorkflowService.fetchReturnedResults(referralTaskUuid);
        if (imports.isEmpty()) {
            throw new IllegalStateException("No returned results available to accept for referral " + referralId);
        }
        // setReferralResult is @Transactional and posts atomically (Observation ->
        // Result, Analysis -> Finalized, Task -> completed); markReferralReconciled is
        // a separate REQUIRES_NEW ack commit. acceptReferral itself stays untransacted
        // so each sub-call keeps its own boundary.
        for (ReferralResultsImportObjects imp : imports) {
            fhirReferralService.setReferralResult(imp);
        }
        referralService.markReferralReconciled(referralId, actorUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceLabReferralDTO> getDashboardReferrals(DashboardView view) {
        List<Referral> referrals = referralDAO.getReferralsByStatus(statusesFor(view)).stream()
                .filter(r -> belongsInBucket(r, view)).toList();
        List<ReferenceLabReferralDTO> dtos = new ArrayList<>(referrals.size());
        for (Referral referral : referrals) {
            dtos.add(toDto(referral, view));
        }
        dtos.sort(Comparator.comparing(ReferenceLabReferralDTO::getSentDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public ReferenceLabMetricsDTO getDashboardMetrics() {
        long outstanding = referralDAO.getReferralsByStatus(OUTSTANDING_STATUSES).size();
        List<Referral> completed = referralDAO.getReferralsByStatus(RETURNED_STATUSES);
        // Returned excludes both actioned cases: manual entries (OGC-799) and
        // Accepted/reconciled rows (OGC-803) — they belong in History, not Returned.
        long returned = completed.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getManuallyEntered()) && !Boolean.TRUE.equals(r.getReconciled()))
                .count();
        long reconciledToday = completed.stream().filter(r -> reconciledOn(r, LocalDate.now())).count();
        long rejectedThisWeek = countRejectedSince(LocalDate.now().minusDays(7));
        return new ReferenceLabMetricsDTO(outstanding, returned, reconciledToday, rejectedThisWeek,
                resolveStuckThresholdDays());
    }

    private int resolveStuckThresholdDays() {
        SiteInformation cfg = siteInformationService.getSiteInformationByName(STUCK_THRESHOLD_CONFIG);
        if (cfg == null || cfg.getValue() == null) {
            return STUCK_THRESHOLD_DEFAULT_DAYS;
        }
        try {
            return Integer.parseInt(cfg.getValue().trim());
        } catch (NumberFormatException e) {
            return STUCK_THRESHOLD_DEFAULT_DAYS;
        }
    }

    /**
     * Splits {@link ReferralStatus#COMPLETED} between the Returned and History
     * buckets. A COMPLETED row needs reception action (Returned) until it's either
     * manually entered (OGC-799) or reconciled/Accepted (OGC-803); once either flag
     * is set it has been actioned and moves to History. Non-COMPLETED rows always
     * belong in whichever bucket their status maps to.
     */
    private boolean belongsInBucket(Referral referral, DashboardView view) {
        if (referral.getStatus() != ReferralStatus.COMPLETED) {
            return true;
        }
        boolean actioned = Boolean.TRUE.equals(referral.getManuallyEntered())
                || Boolean.TRUE.equals(referral.getReconciled());
        return switch (view) {
        case RETURNED -> !actioned;
        case HISTORY -> actioned;
        case OUTSTANDING -> false;
        };
    }

    private long countRejectedSince(LocalDate cutoff) {
        Instant cutoffInstant = cutoff.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return referralDAO.getReferralsByStatus(Collections.singletonList(ReferralStatus.REJECTED)).stream()
                .filter(r -> r.getLastupdated() != null && !r.getLastupdated().toInstant().isBefore(cutoffInstant))
                .count();
    }

    private boolean reconciledOn(Referral referral, LocalDate day) {
        if (!Boolean.TRUE.equals(referral.getReconciled()) || referral.getReconciledAt() == null) {
            return false;
        }
        LocalDate reconciledDay = referral.getReconciledAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return reconciledDay.equals(day);
    }

    private List<ReferralStatus> statusesFor(DashboardView view) {
        return switch (view) {
        case OUTSTANDING -> OUTSTANDING_STATUSES;
        case RETURNED -> RETURNED_STATUSES;
        case HISTORY -> HISTORY_STATUSES;
        };
    }

    private ReferenceLabReferralDTO toDto(Referral referral, DashboardView view) {
        ReferenceLabReferralDTO dto = new ReferenceLabReferralDTO();
        dto.setId(referral.getId());
        dto.setStatus(toFhirStatus(referral.getStatus()));
        dto.setPriority(referral.getPriority());
        dto.setRequestor(referral.getRequesterName());
        dto.setSentDate(toIso(referral.getSentDate()));
        dto.setBoxReceivedDate(toIso(latestChangedAt(referral.getId(), ReferralStatus.RECEIVED)));
        dto.setFhirTaskUuid(referral.getFhirUuid() == null ? null : referral.getFhirUuid().toString());
        dto.setManuallyEntered(Boolean.TRUE.equals(referral.getManuallyEntered()));

        Analysis analysis = referral.getAnalysis();
        if (analysis != null) {
            Sample sample = analysis.getSampleItem() != null ? analysis.getSampleItem().getSample() : null;
            if (sample != null) {
                dto.setLabNumber(sample.getAccessionNumber());
                dto.setCollectedDate(toIso(sample.getCollectionDate()));
                Patient patient = sampleHumanService.getPatientForSample(sample);
                if (patient != null) {
                    dto.setPatientDisplay(formatPatient(patient));
                    dto.setPatientGender(patient.getGender());
                    dto.setPatientAge(ageInYears(patient.getBirthDate()));
                }
            }
            if (analysis.getSampleItem() != null && analysis.getSampleItem().getTypeOfSample() != null) {
                dto.setSampleType(analysis.getSampleItem().getTypeOfSample().getLocalizedName());
            }
            if (analysis.getTest() != null && analysis.getTest().getLocalizedTestName() != null) {
                dto.setTests(Collections.singletonList(analysis.getTest().getLocalizedTestName().getLocalizedValue()));
            }
        }
        if (dto.getTests() == null) {
            dto.setTests(Collections.emptyList());
        }

        Organization organization = referral.getOrganization();
        if (organization != null) {
            dto.setReferenceLabId(organization.getId());
            dto.setReferenceLabName(organization.getOrganizationName());
        }

        ShippingBox box = referral.getAssignedBox();
        if (box != null) {
            dto.setBoxId(box.getBoxId());
        }

        if (view == DashboardView.OUTSTANDING && referral.getSentDate() != null) {
            dto.setDaysOutstanding(daysBetween(referral.getSentDate(), Timestamp.from(Instant.now())));
        } else if (view == DashboardView.RETURNED) {
            dto.setReturnedDate(toIso(latestChangedAt(referral.getId(), ReferralStatus.COMPLETED)));
            enrichReturnedResults(dto, referral);
        } else if (view == DashboardView.HISTORY) {
            Timestamp closed = closedDateFor(referral);
            dto.setClosedDate(toIso(closed));
            dto.setOutcome(outcomeFor(referral));
            if (referral.getSentDate() != null && closed != null) {
                dto.setDaysTotal(daysBetween(referral.getSentDate(), closed));
            }
        }

        return dto;
    }

    // OGC-802: live-read the returned Observations from the remote FHIR store and
    // map
    // them to display-only result cards. Best-effort — a FHIR outage leaves the row
    // listed without cards rather than failing the whole dashboard.
    // ponytail: one FHIR round-trip per returned row; batch/parallelize if the
    // Returned list grows large enough to feel slow.
    private void enrichReturnedResults(ReferenceLabReferralDTO dto, Referral referral) {
        if (referral.getFhirUuid() == null) {
            return;
        }
        try {
            List<ReferenceLabReferralDTO.ResultCard> cards = new ArrayList<>();
            for (ReferralResultsImportObjects imp : fhirApiWorkflowService
                    .fetchReturnedResults(referral.getFhirUuid())) {
                if (imp.observations == null) {
                    continue;
                }
                for (Observation observation : imp.observations) {
                    cards.add(toResultCard(observation));
                }
            }
            if (!cards.isEmpty()) {
                dto.setResults(cards);
                dto.setResultSummary(summarizeResults(cards));
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "enrichReturnedResults",
                    "could not live-read results for referral " + referral.getId() + ": " + e.getMessage());
        }
    }

    private ReferenceLabReferralDTO.ResultCard toResultCard(Observation obs) {
        ReferenceLabReferralDTO.ResultCard card = new ReferenceLabReferralDTO.ResultCard();
        if (obs.hasCode()) {
            card.setTestName(
                    obs.getCode().hasText() ? obs.getCode().getText() : obs.getCode().getCodingFirstRep().getDisplay());
        }
        if (obs.hasValueQuantity()) {
            Quantity q = obs.getValueQuantity();
            String number = q.getValue() != null ? q.getValue().stripTrailingZeros().toPlainString() : "";
            String comparator = q.hasComparator() ? q.getComparator().toCode() + " " : "";
            card.setValue((comparator + number).trim());
            card.setUnits(q.getUnit());
        } else if (obs.hasValueStringType()) {
            card.setValue(obs.getValueStringType().getValue());
        } else if (obs.hasValueCodeableConcept()) {
            CodeableConcept cc = obs.getValueCodeableConcept();
            card.setValue(cc.hasText() ? cc.getText() : cc.getCodingFirstRep().getDisplay());
        }
        if (obs.hasReferenceRange()) {
            card.setReferenceRange(referenceRangeText(obs.getReferenceRangeFirstRep()));
        }
        card.setInterpretation(interpretationLabel(obs));
        if (obs.hasNote()) {
            card.setNote(obs.getNoteFirstRep().getText());
        }
        return card;
    }

    private String referenceRangeText(Observation.ObservationReferenceRangeComponent rr) {
        if (rr.hasText()) {
            return rr.getText();
        }
        String low = rr.hasLow() && rr.getLow().getValue() != null
                ? rr.getLow().getValue().stripTrailingZeros().toPlainString()
                : null;
        String high = rr.hasHigh() && rr.getHigh().getValue() != null
                ? rr.getHigh().getValue().stripTrailingZeros().toPlainString()
                : null;
        if (low != null && high != null) {
            return low + " – " + high;
        }
        if (high != null) {
            return "≤ " + high;
        }
        if (low != null) {
            return "≥ " + low;
        }
        return null;
    }

    // HL7 v3 ObservationInterpretation → display label. AA/HH/LL = Critical,
    // A/H/L/AB = Abnormal, N = Normal; otherwise fall back to any free text.
    private String interpretationLabel(Observation obs) {
        for (CodeableConcept interpretation : obs.getInterpretation()) {
            for (Coding coding : interpretation.getCoding()) {
                String code = coding.getCode() == null ? "" : coding.getCode().toUpperCase();
                if (code.equals("AA") || code.equals("HH") || code.equals("LL")) {
                    return "Critical";
                }
                if (code.equals("A") || code.equals("H") || code.equals("L") || code.equals("AB")) {
                    return "Abnormal";
                }
                if (code.equals("N")) {
                    return "Normal";
                }
            }
            if (interpretation.hasText()) {
                return interpretation.getText();
            }
        }
        return null;
    }

    private String summarizeResults(List<ReferenceLabReferralDTO.ResultCard> cards) {
        ReferenceLabReferralDTO.ResultCard first = cards.get(0);
        String summary = first.getValue() == null ? "" : first.getValue();
        if (first.getUnits() != null && !first.getUnits().isBlank()) {
            summary = (summary + " " + first.getUnits()).trim();
        }
        if (cards.size() > 1) {
            summary = summary + " (+" + (cards.size() - 1) + " more)";
        }
        return summary;
    }

    private Integer ageInYears(Timestamp birthDate) {
        if (birthDate == null) {
            return null;
        }
        java.time.LocalDate dob = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(dob, LocalDate.now()).getYears();
    }

    /**
     * Returns the {@code changedAt} of the most recent ReferralStatusHistory row
     * that landed on {@code toStatus}, or {@code null} for legacy referrals with no
     * history (predating S-14 / OGC-797 transition writers).
     */
    private Timestamp latestChangedAt(String referralId, ReferralStatus toStatus) {
        List<ReferralStatusHistory> history;
        try {
            history = statusHistoryDAO.findByReferralIdOrderedByChangedAt(referralId);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ReferralStatusHistory row = history.get(i);
            if (toStatus == row.getToStatus()) {
                return row.getChangedAt();
            }
        }
        return null;
    }

    private String formatPatient(Patient patient) {
        Person person = patient.getPerson();
        if (person == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        if (person.getLastName() != null) {
            name.append(person.getLastName());
        }
        if (person.getFirstName() != null) {
            if (name.length() > 0) {
                name.append(", ");
            }
            name.append(person.getFirstName());
        }
        String gender = patient.getGender();
        if (gender != null && !gender.isBlank()) {
            name.append(" (").append(gender).append(")");
        }
        return name.length() == 0 ? null : name.toString();
    }

    @SuppressWarnings("deprecation")
    private String toFhirStatus(ReferralStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
        case DRAFT -> "draft";
        case REQUESTED, SENT, CREATED -> "requested";
        case RECEIVED -> "received";
        case IN_PROGRESS -> "in-progress";
        case COMPLETED, FINISHED -> "completed";
        case REJECTED -> "rejected";
        case CANCELLED, CANCELED -> "cancelled";
        };
    }

    @SuppressWarnings("deprecation")
    private String outcomeFor(Referral referral) {
        if (Boolean.TRUE.equals(referral.getLostStatus())) {
            return "Lost";
        }
        if (referral.getStatus() == null) {
            return null;
        }
        return switch (referral.getStatus()) {
        case REJECTED -> "Rejected";
        case CANCELLED, CANCELED -> "Cancelled";
        case COMPLETED, FINISHED -> "Reconciled";
        default -> null;
        };
    }

    private Timestamp closedDateFor(Referral referral) {
        // Lost is a Sample Shipment flag, not a referral-state transition — read
        // it from the dedicated column.
        if (Boolean.TRUE.equals(referral.getLostStatus()) && referral.getLostDate() != null) {
            return referral.getLostDate();
        }
        // Accept/reconcile closes the row at reconciled_at, not the COMPLETED
        // transition time (which is when the result returned, i.e. the open moment).
        if (referral.getStatus() == ReferralStatus.COMPLETED && Boolean.TRUE.equals(referral.getReconciled())
                && referral.getReconciledAt() != null) {
            return referral.getReconciledAt();
        }
        ReferralStatus terminal = referral.getStatus();
        if (terminal == ReferralStatus.REJECTED || terminal == ReferralStatus.CANCELLED
                || terminal == ReferralStatus.COMPLETED) {
            Timestamp fromHistory = latestChangedAt(referral.getId(), terminal);
            if (fromHistory != null) {
                return fromHistory;
            }
        }
        // Legacy fallbacks for rows predating the status-history writers.
        if (referral.getCancelDate() != null) {
            return referral.getCancelDate();
        }
        return referral.getResultRecievedDate();
    }

    private long daysBetween(Timestamp start, Timestamp end) {
        return Duration.between(start.toInstant(), end.toInstant()).toDays();
    }

    private String toIso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}
