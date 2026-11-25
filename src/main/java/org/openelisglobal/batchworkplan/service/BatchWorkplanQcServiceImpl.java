package org.openelisglobal.batchworkplan.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcDAO;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcReleaseDAO;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.ControlLotQcResponse;
import org.openelisglobal.batchworkplan.form.QcRunResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BatchWorkplanQcServiceImpl implements BatchWorkplanQcService {

    static final int RECENT_RUNS = 5;

    private static final List<String> FINAL_STATUSES = List.of(BatchQcEvaluator.RESULT_ACCEPTED,
            BatchQcEvaluator.RESULT_REJECTED);
    private static final List<String> ACCEPTED_ONLY = List.of(BatchQcEvaluator.RESULT_ACCEPTED);
    private static final String RESULT_PENDING = "PENDING";
    private static final Set<BatchQcStatus> INLINE_ENTRY_STATUSES = Set.of(BatchQcStatus.OVERDUE, BatchQcStatus.FAILED,
            BatchQcStatus.VIOLATION, BatchQcStatus.NOT_RUN);

    private final BatchWorkplanQcDAO qcDAO;
    private final BatchWorkplanQcReleaseDAO releaseDAO;
    private final SystemUserService systemUserService;
    private Clock clock = Clock.systemDefaultZone();

    public BatchWorkplanQcServiceImpl(BatchWorkplanQcDAO qcDAO, BatchWorkplanQcReleaseDAO releaseDAO,
            SystemUserService systemUserService) {
        this.qcDAO = qcDAO;
        this.releaseDAO = releaseDAO;
        this.systemUserService = systemUserService;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public BatchQcStatusResponse evaluate(Collection<BatchTestRef> tests, String instrumentId) {
        String instrument = StringUtils.trimToNull(instrumentId);
        ZonedDateTime now = ZonedDateTime.now(clock);
        Map<String, String> userNames = new HashMap<>();

        BatchQcStatus overall = null;
        boolean pending = false;
        List<ControlLotQcResponse> lots = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();
        for (BatchTestRef test : distinct(tests)) {
            List<QCControlLot> candidates = selectLots(qcDAO.getControlLots(test.testId(), instrument), now);
            if (candidates.isEmpty()) {
                uncovered.add(test.testName());
                overall = BatchQcStatus.worst(overall, BatchQcStatus.NO_CONTROL_LOT);
                continue;
            }
            for (QCControlLot lot : candidates) {
                ControlLotQcResponse lotStatus = evaluateLot(lot, test, now, userNames);
                lots.add(lotStatus);
                overall = BatchQcStatus.worst(overall, lotStatus.status());
                pending |= lotStatus.evaluationPending();
            }
        }
        if (overall == null) {
            overall = BatchQcStatus.NO_CONTROL_LOT;
        }
        String instrumentName = instrument == null ? null : qcDAO.getAnalyzerNames(List.of(instrument)).get(instrument);
        return new BatchQcStatusResponse(overall, overall.isBlocking(), !overall.isPass() && !overall.isBlocking(),
                pending, instrument, instrumentName, lots, uncovered);
    }

    /**
     * The lots a release is checked against. Usable ACTIVE lots when there are any,
     * one per control level; otherwise lots still in establishment; otherwise the
     * most recently activated retired lot, so the tile can say which lot expired.
     */
    static List<QCControlLot> selectLots(List<QCControlLot> all, ZonedDateTime now) {
        List<QCControlLot> active = all.stream().filter(
                lot -> BatchQcEvaluator.LOT_ACTIVE.equals(lot.getStatus()) && !BatchQcEvaluator.isRetired(lot, now))
                .collect(Collectors.toList());
        if (!active.isEmpty()) {
            return active;
        }
        List<QCControlLot> establishing = all.stream()
                .filter(lot -> BatchQcEvaluator.LOT_ESTABLISHMENT.equals(lot.getStatus())
                        && !BatchQcEvaluator.isRetired(lot, now))
                .collect(Collectors.toList());
        if (!establishing.isEmpty()) {
            return establishing;
        }
        return all.isEmpty() ? List.of() : List.of(all.get(0));
    }

    private ControlLotQcResponse evaluateLot(QCControlLot lot, BatchTestRef test, ZonedDateTime now,
            Map<String, String> userNames) {
        List<QCResult> recent = qcDAO.getRecentResults(lot.getId(), RECENT_RUNS);
        QCResult latestFinal = qcDAO.getLatestInStatuses(lot.getId(), FINAL_STATUSES);
        QCResult latestAccepted = qcDAO.getLatestInStatuses(lot.getId(), ACCEPTED_ONLY);
        List<QCRuleViolation> openRejections = qcDAO.getOpenRejections(lot.getId());
        Timestamp lastRelease = releaseDAO.getLastReleaseAt(lot.getId());

        BatchQcStatus status = BatchQcEvaluator.evaluate(lot, latestFinal, latestAccepted, openRejections, lastRelease,
                now);

        List<String> resultIds = recent.stream().map(QCResult::getId).collect(Collectors.toList());
        if (latestFinal != null && !resultIds.contains(latestFinal.getId())) {
            resultIds.add(latestFinal.getId());
        }
        Map<String, List<QCRuleViolation>> violationsByResult = qcDAO.getViolationsForResults(resultIds);

        Set<String> ruleCodes = new LinkedHashSet<>();
        if (status == BatchQcStatus.VIOLATION) {
            openRejections.forEach(violation -> ruleCodes.add(violation.getRuleCode()));
        } else if (status == BatchQcStatus.FAILED && latestFinal != null) {
            violationsByResult.getOrDefault(latestFinal.getId(), List.of())
                    .forEach(violation -> ruleCodes.add(violation.getRuleCode()));
        }

        List<QcRunResponse> runs = recent.stream().map(run -> toRun(run, violationsByResult, userNames))
                .collect(Collectors.toList());
        QcRunResponse lastRun = latestFinal == null ? (runs.isEmpty() ? null : runs.get(0))
                : toRun(latestFinal, violationsByResult, userNames);
        boolean pending = !recent.isEmpty() && RESULT_PENDING.equals(recent.get(0).getResultStatus())
                && BatchQcEvaluator.LOT_ACTIVE.equals(lot.getStatus());
        Timestamp lastPassingAt = latestAccepted == null ? null : latestAccepted.getRunDateTime();
        ZonedDateTime nextDue = BatchQcEvaluator.nextDue(lot, lastPassingAt, now);
        boolean bench = StringUtils.isBlank(lot.getInstrumentId());

        return new ControlLotQcResponse(lot.getId(), lot.getLotNumber(), lot.getProductName(), lot.getManufacturer(),
                lot.getControlLevel(), test.testId(), test.testName(), test.testSectionId(), lot.getInstrumentId(),
                bench, lot.getStatus(), lot.getExpirationDate(), lot.getQcFrequencyType(), lot.getQcFrequencyHours(),
                lot.getShiftDefinitionHours(), !Boolean.FALSE.equals(lot.getQcRequired()), status, lastPassingAt,
                nextDue == null ? null : Timestamp.from(nextDue.toInstant()), pending,
                bench && INLINE_ENTRY_STATUSES.contains(status), new ArrayList<>(ruleCodes),
                openRejections.stream().map(QCRuleViolation::getId).collect(Collectors.toList()), lastRun, runs);
    }

    private QcRunResponse toRun(QCResult run, Map<String, List<QCRuleViolation>> violationsByResult,
            Map<String, String> userNames) {
        List<String> codes = violationsByResult.getOrDefault(run.getId(), List.of()).stream()
                .map(QCRuleViolation::getRuleCode).distinct().collect(Collectors.toList());
        Integer performer = run.getTechnicianId() != null ? run.getTechnicianId() : run.getSystemUserId();
        return new QcRunResponse(run.getId(), run.getRunDateTime(), userName(performer, userNames),
                run.getResultValue(), run.getUnitOfMeasure(), run.getZScore(), run.getResultStatus(),
                run.getQualitativeOutcome() == null ? null : run.getQualitativeOutcome().name(), codes);
    }

    private String userName(Integer userId, Map<String, String> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(String.valueOf(userId), id -> {
            try {
                SystemUser user = systemUserService.getUserById(id);
                if (user == null) {
                    return id;
                }
                String name = (StringUtils.trimToEmpty(user.getFirstName()) + " "
                        + StringUtils.trimToEmpty(user.getLastName())).trim();
                return StringUtils.defaultIfBlank(name, id);
            } catch (RuntimeException e) {
                return id;
            }
        });
    }

    private static Collection<BatchTestRef> distinct(Collection<BatchTestRef> tests) {
        Map<String, BatchTestRef> byId = new LinkedHashMap<>();
        for (BatchTestRef test : tests) {
            if (test != null && test.testId() != null) {
                byId.putIfAbsent(test.testId(), test);
            }
        }
        return new ArrayList<>(byId.values());
    }
}
