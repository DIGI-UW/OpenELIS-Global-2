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
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
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
        // Returned excludes manual entries — those land directly in History per
        // OGC-799 AC ("After manual save, row jumps from Outstanding → History").
        long returned = referralDAO.getReferralsByStatus(RETURNED_STATUSES).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getManuallyEntered())).count();
        long rejectedThisWeek = countRejectedSince(LocalDate.now().minusDays(7));
        // reconciledToday wires up in v2 when the reconciled audit lands; until then
        // it stays at 0 so the tile renders without faking a count.
        return new ReferenceLabMetricsDTO(outstanding, returned, 0L, rejectedThisWeek, resolveStuckThresholdDays());
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
     * buckets by the {@code manually_entered} flag. Non-COMPLETED rows always
     * belong in whichever bucket their status maps to.
     */
    private boolean belongsInBucket(Referral referral, DashboardView view) {
        if (referral.getStatus() != ReferralStatus.COMPLETED) {
            return true;
        }
        boolean manuallyEntered = Boolean.TRUE.equals(referral.getManuallyEntered());
        return switch (view) {
        case RETURNED -> !manuallyEntered;
        case HISTORY -> manuallyEntered;
        case OUTSTANDING -> false;
        };
    }

    private long countRejectedSince(LocalDate cutoff) {
        Instant cutoffInstant = cutoff.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return referralDAO.getReferralsByStatus(Collections.singletonList(ReferralStatus.REJECTED)).stream()
                .filter(r -> r.getLastupdated() != null && !r.getLastupdated().toInstant().isBefore(cutoffInstant))
                .count();
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
