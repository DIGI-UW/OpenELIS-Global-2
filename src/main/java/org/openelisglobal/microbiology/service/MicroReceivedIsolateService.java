package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroReceivedIsolateService {
    private final MicroCaseDAO cases;
    private final MicroIsolateDAO isolates;
    private final MicroCaseInoculationDAO cultures;
    private final MicroCaseActivityDAO activities;
    private final MicroCaseAmendmentDAO amendments;

    public MicroReceivedIsolateService(MicroCaseDAO cases, MicroIsolateDAO isolates, MicroCaseInoculationDAO cultures,
            MicroCaseActivityDAO activities, MicroCaseAmendmentDAO amendments) {
        this.cases = cases;
        this.isolates = isolates;
        this.cultures = cultures;
        this.activities = activities;
        this.amendments = amendments;
    }

    @Transactional
    public void receive(String caseId, SampleItem sample, String actor) {
        if (sample.getTypeOfSample() == null || !sample.getTypeOfSample().isMicrobiologyIsolate()
                || sample.getParentSampleItem() != null)
            return;
        var c = cases.getForUpdate(caseId);
        MicroCaseMutationGuard.requireMutable(c);
        if (isolates.getAllMatching("caseId", caseId).stream()
                .anyMatch(i -> sample.getId().equals(i.getSourceSampleItemId())))
            return;
        var at = sample.getReceivedDate() == null ? sample.getSample().getReceivedTimestamp()
                : sample.getReceivedDate();
        if (at == null)
            at = MicroCaseServiceImpl.now();
        var activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType("RECEIVED_ISOLATE");
        activity.setPerformedBy(actor);
        activity.setOccurredAt(at);
        activity.setSysUserId(actor);
        activities.insert(activity);
        var row = new MicroCaseInoculation();
        row.setCaseId(caseId);
        row.setSourceSampleItemId(sample.getId());
        row.setActivityId(activity.getId());
        row.setContainerIdentifier(
                sample.getExternalId() == null ? sample.getSample().getAccessionNumber() + "-" + sample.getSortOrder()
                        : sample.getExternalId());
        row.setMedia("Received isolate");
        row.setNotTracked(true);
        row.setOccurredAt(at);
        row.setPerformedBy(actor);
        row.setOutcome("GROWTH");
        row.setOutcomeAt(at);
        row.setOutcomeBy(actor);
        row.setPositiveSource("RECEIVED_ISOLATE");
        row.setSysUserId(actor);
        cultures.insert(row);
        var isolate = new MicroIsolate();
        isolate.setCaseId(caseId);
        var existing = isolates.getAllMatching("caseId", caseId);
        int number = 1;
        while (true) {
            final String label = "ISO-" + number;
            if (existing.stream().noneMatch(i -> label.equals(i.getIsolateLabel())))
                break;
            number++;
        }
        isolate.setIsolateLabel("ISO-" + number);
        isolate.setSourceCultureId(row.getId());
        isolate.setSourceCulture(row);
        isolate.setSourceSampleItemId(sample.getId());
        isolate.setSampleItemId(sample.getId());
        var amendment = amendments.getOpenByCaseId(caseId);
        if (amendment != null)
            isolate.setAmendmentId(amendment.getId());
        isolate.setCreatedBy(actor);
        isolate.setCreatedAt(at);
        isolate.setSysUserId(actor);
        isolates.insert(isolate);
        if (amendment == null)
            c.setStage(MicroCaseStage.IDENTIFICATION.name());
        c.setSysUserId(actor);
        cases.update(c);
    }
}
