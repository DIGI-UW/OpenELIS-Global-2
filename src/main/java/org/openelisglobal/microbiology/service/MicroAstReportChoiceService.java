package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroAstReportChoiceService {
    private final MicroAstReportChoiceDAO choices;
    private final MicroAstReadingDAO readings;
    private final MicroAstRunDAO runs;
    private final MicroIsolateDAO isolates;
    private final MicroCaseActivityDAO activities;
    private final MicroCaseAuthorizationService authorization;

    public MicroAstReportChoiceService(MicroAstReportChoiceDAO choices, MicroAstReadingDAO readings,
            MicroAstRunDAO runs, MicroIsolateDAO isolates, MicroCaseActivityDAO activities,
            MicroCaseAuthorizationService authorization) {
        this.choices = choices;
        this.readings = readings;
        this.runs = runs;
        this.isolates = isolates;
        this.activities = activities;
        this.authorization = authorization;
    }

    public List<MicroAstReportChoice> getChoices(String isolateId) {
        return choices.getByIsolateId(isolateId);
    }

    public List<MicroAstReading> getSelectedReadings(String isolateId) {
        return choices.getByIsolateId(isolateId).stream().map(c -> eligible(isolateId, c.getReadingId())).toList();
    }

    @Transactional
    public MicroAstReportChoice select(String isolateId, String antibioticId, String readingId, String reason,
            String actor) {
        var isolate = isolates.get(isolateId).orElseThrow();
        authorization.requireWrite(isolate.getCaseId(), actor, Constants.ROLE_VALIDATION);
        MicroCaseServiceImpl.requireText(reason, "reason");
        var reading = eligible(isolateId, readingId);
        if (!Objects.equals(antibioticId, reading.getAntibioticId()))
            throw new MicroAstConflictException("AST_REPORT_AGENT_MISMATCH");
        var choice = choices.getByIsolateId(isolateId).stream().filter(c -> antibioticId.equals(c.getAntibioticId()))
                .findFirst().orElse(null);
        String previous = choice == null ? null : choice.getReadingId();
        if (choice == null) {
            choice = new MicroAstReportChoice();
            choice.setIsolateId(isolateId);
            choice.setAntibioticId(antibioticId);
        }
        choice.setReadingId(readingId);
        choice.setSelectedBy(actor);
        choice.setSelectedAt(MicroCaseServiceImpl.now());
        if (previous == null)
            choices.insert(choice);
        else
            choices.update(choice);
        var event = new MicroCaseActivity();
        event.setCaseId(isolate.getCaseId());
        event.setActivityType("AST_REPORTABLE_SELECTED");
        event.setPerformedBy(actor);
        event.setOccurredAt(MicroCaseServiceImpl.now());
        event.setNote(reason.trim());
        event.setStructuredData("{\"isolateId\":\"" + isolateId + "\",\"antibioticId\":\"" + antibioticId
                + "\",\"beforeReadingId\":\"" + previous + "\",\"afterReadingId\":\"" + readingId + "\"}");
        activities.insert(event);
        synchronize(isolateId);
        return choice;
    }

    @Transactional
    public void defaultFromReviewedRun(MicroAstRun run, String actor) {
        if (!"ORGANISM_DEFAULT".equals(run.getPanelProvenance()))
            return;
        var chosen = getChoices(run.getIsolateId());
        for (var reading : latest(run.getId())) {
            if (chosen.stream().anyMatch(c -> c.getAntibioticId().equals(reading.getAntibioticId())))
                continue;
            var c = new MicroAstReportChoice();
            c.setIsolateId(run.getIsolateId());
            c.setAntibioticId(reading.getAntibioticId());
            c.setReadingId(reading.getId());
            c.setSelectedBy(actor);
            c.setSelectedAt(MicroCaseServiceImpl.now());
            choices.insert(c);
        }
    }

    public List<MicroAstReading> latest(String runId) {
        Map<String, MicroAstReading> latest = new LinkedHashMap<>();
        for (var r : readings.getByRunId(runId))
            latest.put(r.getAntibioticId(), r);
        return List.copyOf(latest.values());
    }

    private MicroAstReading eligible(String isolateId, String readingId) {
        var r = readings.get(readingId).orElseThrow(() -> new IllegalArgumentException("AST reading not found"));
        var run = runs.get(r.getAstRunId()).orElseThrow();
        var isolate = isolates.get(isolateId).orElseThrow();
        if (!isolateId.equals(run.getIsolateId()) || isolate.getCancelledAt() != null
                || !"REVIEWED".equals(run.getStatus())
                || latest(run.getId()).stream().noneMatch(candidate -> readingId.equals(candidate.getId())))
            throw new MicroAstConflictException("AST_REPORT_READING_NOT_VALIDATED");
        return r;
    }

    private void synchronize(String isolateId) {
        var selected = getSelectedReadings(isolateId).stream().map(MicroAstReading::getAstRunId)
                .collect(java.util.stream.Collectors.toSet());
        for (var run : runs.getByIsolateId(isolateId)) {
            run.setReportable(selected.contains(run.getId()));
            runs.update(run);
        }
    }

    @Transactional
    public void restore(String caseId, org.openelisglobal.microbiology.form.MicroPatientReportForm baseline,
            String actor) {
        for (var isolate : isolates.getByCaseId(caseId)) {
            var expected = baseline.isolates.stream().filter(i -> i.id().equals(isolate.getId()))
                    .flatMap(i -> i.susceptibilities().stream()).toList();
            for (var choice : choices.getByIsolateId(isolate.getId())) {
                var prior = expected.stream().filter(r -> r.antibioticId().equals(choice.getAntibioticId())).findFirst()
                        .orElse(null);
                if (prior == null)
                    choices.delete(choice);
                else {
                    choice.setReadingId(prior.readingId());
                    choice.setSelectedBy(actor);
                    choice.setSelectedAt(MicroCaseServiceImpl.now());
                    choices.update(choice);
                }
            }
            if (isolate.getCancelledAt() == null)
                synchronize(isolate.getId());
        }
    }

    public static String interpretation(MicroAstReading r) {
        return r.getOverrideInterpretation() == null ? r.getInterpretation() : r.getOverrideInterpretation();
    }
}
