package org.openelisglobal.resultrun.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.form.ResultRunException;
import org.openelisglobal.resultrun.form.ResultRunExceptionKind;
import org.openelisglobal.resultrun.form.RunOrderMatch;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.service.ResultRunRowClassifier.ClassifiedRow;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunExceptionServiceImpl implements RunExceptionService {

    static final int REASON_MAX = 200;
    static final int QUERY_MIN = 3;
    private static final int ORDERS_MAX = 10;
    private static final int DETAIL_MAX = 255;
    private static final Set<String> RESOLVING_EVENTS = Set.of(ResultRunEvent.EXCEPTION_HELD,
            ResultRunEvent.EXCEPTION_MATCHED);

    @Lazy
    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private ResultRunEventDAO resultRunEventDAO;
    @Lazy
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private ResultRunRowClassifier rowClassifier;
    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional(readOnly = true)
    public List<ResultRunException> exceptionsOf(Long runId, String analyzerId, List<ClassifiedRow> rows) {
        Map<String, List<ClassifiedRow>> groups = new LinkedHashMap<>();
        rows.stream().filter(row -> row.exceptionKind() != null)
                .sorted(Comparator.comparing(ClassifiedRow::exceptionKind))
                .forEach(row -> groups.computeIfAbsent(keyOf(row), key -> new ArrayList<>()).add(row));
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<String, ResultRunEvent> holds = holdsByRow(runId);
        Map<String, String> names = new HashMap<>();
        Map<String, String> testNames = new HashMap<>();
        return groups.entrySet().stream().map(group -> {
            ClassifiedRow first = group.getValue().get(0);
            AnalyzerResults row = first.row();
            List<String> rowIds = group.getValue().stream().map(member -> member.row().getId()).toList();
            ResultRunException.Hold hold = rowIds.stream().map(holds::get).filter(Objects::nonNull)
                    .max(Comparator.comparing(ResultRunEvent::getOccurredAt))
                    .map(event -> new ResultRunException.Hold(reasonOf(event),
                            names.computeIfAbsent(StringUtils.defaultString(event.getActorId()), this::userName),
                            event.getOccurredAt().toInstant().toString()))
                    .orElse(null);
            return new ResultRunException(group.getKey(), first.exceptionKind(), row.getAccessionNumber(),
                    row.getTestId(), rowClassifier.testNameOf(row, testNames), codeOf(row), valueOf(row),
                    row.getImportIssueReason(), rowIds, mappingOf(first, analyzerId), hold);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunOrderMatch> ordersMatching(String query, String testId) {
        String prefix = StringUtils.trimToEmpty(query);
        if (prefix.length() < QUERY_MIN) {
            return List.of();
        }
        return sampleService.getSamplesWithAccessionNumberStartingWith(prefix, ORDERS_MAX).stream()
                .map(sample -> new RunOrderMatch(sample.getAccessionNumber(), patientNameOf(sample),
                        sample.getReceivedDateForDisplay(),
                        StringUtils.isNotBlank(testId) && rowClassifier.isOrdered(sample, testId)))
                .toList();
    }

    @Override
    @Transactional
    public void match(Long runId, RunViewer viewer, List<String> rowIds, String key, String accessionNumber,
            String reason, String sysUserId) {
        String why = requiredReason(reason);
        String target = StringUtils.trimToNull(accessionNumber);
        Sample sample = target == null ? null : sampleService.getSampleByAccessionNumber(target);
        if (sample == null) {
            throw new IllegalArgumentException("No order has lab number " + StringUtils.defaultString(target));
        }
        List<ClassifiedRow> rows = exceptionRows(runId, viewer, rowIds, key,
                Set.of(ResultRunExceptionKind.UNMATCHED, ResultRunExceptionKind.UNORDERED));
        for (ClassifiedRow classified : rows) {
            AnalyzerResults row = classified.row();
            if (sample.getAccessionNumber().equals(row.getAccessionNumber())) {
                throw new IllegalArgumentException("The value is already on lab number " + target);
            }
        }
        for (ClassifiedRow classified : rows) {
            AnalyzerResults row = classified.row();
            String reported = StringUtils.defaultIfBlank(row.getInstrumentSpecimenId(), row.getAccessionNumber());
            row.setAccessionNumber(sample.getAccessionNumber());
            row.setInstrumentSpecimenId(sample.getAccessionNumber());
            row.setSysUserId(sysUserId);
            analyzerResultsService.update(row);
            log(runId, ResultRunEvent.EXCEPTION_MATCHED, row,
                    StringUtils.defaultString(reported) + " -> " + sample.getAccessionNumber() + ": " + why, sysUserId);
        }
    }

    @Override
    @Transactional
    public void hold(Long runId, RunViewer viewer, List<String> rowIds, String key, String reason, String sysUserId) {
        String why = requiredReason(reason);
        for (ClassifiedRow classified : exceptionRows(runId, viewer, rowIds, key,
                Set.of(ResultRunExceptionKind.values()))) {
            log(runId, ResultRunEvent.EXCEPTION_HELD, classified.row(), why, sysUserId);
        }
    }

    @Override
    @Transactional
    public void ignore(Long runId, RunViewer viewer, List<String> rowIds, String key, String reason, String sysUserId) {
        String why = requiredReason(reason);
        for (ClassifiedRow classified : exceptionRows(runId, viewer, rowIds, key,
                Set.of(ResultRunExceptionKind.values()))) {
            AnalyzerResults row = classified.row();
            log(runId, ResultRunEvent.EXCEPTION_IGNORED, row,
                    classified.exceptionKind().name() + " " + describe(row) + ": " + why, sysUserId);
            row.setSysUserId(sysUserId);
            analyzerResultsService.delete(row);
        }
    }

    /**
     * The values asked for, each still waiting in this run under the record the
     * page showed and of one of the given kinds, so a decision never lands on a
     * value someone else has just resolved or moved to another record.
     */
    private List<ClassifiedRow> exceptionRows(Long runId, RunViewer viewer, List<String> rowIds, String key,
            Set<ResultRunExceptionKind> kinds) {
        if (rowIds == null || rowIds.isEmpty()) {
            throw new IllegalArgumentException("Choose the records to resolve");
        }
        ResultRun run = resultRunService.get(runId);
        if (run == null || !run.getState().isActive()) {
            throw new IllegalStateException("This run is no longer open");
        }
        Map<String, ClassifiedRow> waiting = resultRunService.classifiedRows(runId, viewer).orElse(List.of()).stream()
                .collect(Collectors.toMap(row -> row.row().getId(), Function.identity()));
        List<ClassifiedRow> rows = new ArrayList<>();
        for (String rowId : rowIds.stream().distinct().toList()) {
            ClassifiedRow row = waiting.get(rowId);
            if (row == null || row.exceptionKind() == null || !kinds.contains(row.exceptionKind())
                    || !keyOf(row).equals(key)) {
                throw new IllegalStateException(
                        "This record has changed since the run was opened. Reload the run to see what is left.");
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Each value's latest hold, unless it was matched since: a hold explains why a
     * record waits, and a match moves the value to another cause.
     */
    private Map<String, ResultRunEvent> holdsByRow(Long runId) {
        Map<String, ResultRunEvent> latest = new HashMap<>();
        for (ResultRunEvent event : resultRunEventDAO.getEvents(runId)) {
            if (event.getRowId() != null && RESOLVING_EVENTS.contains(event.getEvent())) {
                latest.put(event.getRowId(), event);
            }
        }
        latest.values().removeIf(event -> !ResultRunEvent.EXCEPTION_HELD.equals(event.getEvent()));
        return latest;
    }

    private void log(Long runId, String eventName, AnalyzerResults row, String detail, String sysUserId) {
        ResultRunEvent event = new ResultRunEvent();
        event.setRunId(runId);
        event.setEvent(eventName);
        event.setTestId(row.getTestId());
        event.setRowId(row.getId());
        event.setDetail(StringUtils.abbreviate(detail, DETAIL_MAX));
        event.setActorId(sysUserId);
        event.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        event.setSysUserId(sysUserId);
        resultRunEventDAO.insert(event);
    }

    static String requiredReason(String reason) {
        String why = StringUtils.trimToNull(reason);
        if (why == null) {
            throw new IllegalArgumentException("A reason is required");
        }
        if (why.length() > REASON_MAX) {
            throw new IllegalArgumentException("A reason is at most " + REASON_MAX + " characters");
        }
        return why;
    }

    private static String keyOf(ClassifiedRow classified) {
        AnalyzerResults row = classified.row();
        return switch (classified.exceptionKind()) {
        case UNMATCHED -> "UNMATCHED|" + StringUtils.defaultString(row.getAccessionNumber());
        case UNORDERED -> "UNORDERED|" + row.getAccessionNumber() + "|" + row.getTestId();
        case UNMAPPED -> "UNMAPPED|" + row.getImportIssueReason() + "|" + codeOf(row) + "|"
                + StringUtils.defaultString(valueOf(row));
        };
    }

    /**
     * Map now opens the analyzer's mapping on the code, as the import page's link
     * does, when the value came through a mapping profile.
     */
    private static ResultRunException.MappingTarget mappingOf(ClassifiedRow classified, String analyzerId) {
        AnalyzerResults row = classified.row();
        if (classified.exceptionKind() != ResultRunExceptionKind.UNMAPPED || analyzerId == null
                || StringUtils.isBlank(row.getRawTestCode()) || StringUtils.isBlank(row.getSourceProfileId())
                || row.getSourceProfileRevision() == null) {
            return null;
        }
        return new ResultRunException.MappingTarget(analyzerId, row.getRawTestCode(),
                StringUtils.trimToNull(row.getRawResultValue()));
    }

    private static String codeOf(AnalyzerResults row) {
        return ResultRunServiceImpl.codeOf(row);
    }

    private static String valueOf(AnalyzerResults row) {
        return StringUtils.defaultIfBlank(row.getRawResultValue(), row.getResult());
    }

    private static String describe(AnalyzerResults row) {
        return StringUtils.defaultString(row.getAccessionNumber()) + " " + StringUtils.defaultString(codeOf(row));
    }

    private static String reasonOf(ResultRunEvent event) {
        return event.getDetail();
    }

    private String userName(String sysUserId) {
        if (StringUtils.isBlank(sysUserId)) {
            return null;
        }
        SystemUser user = systemUserService.get(sysUserId);
        return user == null ? null : StringUtils.trim(user.getNameForDisplay());
    }

    private String patientNameOf(Sample sample) {
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient == null || patient.getPerson() == null) {
            return null;
        }
        return StringUtils.trimToNull(patientService.getLastFirstName(patient));
    }
}
