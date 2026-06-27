package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWorklistServiceImpl implements MicroWorklistService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroCriticalCommunicationDAO communicationDAO;

    public MicroWorklistServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroCriticalCommunicationDAO communicationDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.communicationDAO = communicationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWorklistRowForm> getWorklistRows() {
        List<MicroWorklistRowForm> rows = new ArrayList<>();
        for (MicroCase microCase : caseDAO.getOpenCases()) {
            rows.add(toRow(microCase));
        }
        rows.sort(Comparator.comparingInt(this::urgencyRank).thenComparingInt(this::actionRank)
                .thenComparing(row -> row.createdAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private MicroWorklistRowForm toRow(MicroCase microCase) {
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(microCase.getId());
        List<MicroCriticalCommunication> communications = communicationDAO.getByCaseId(microCase.getId());
        MicroWorklistRowForm row = new MicroWorklistRowForm();
        row.caseId = microCase.getId();
        row.sampleItemId = microCase.getSampleItemId();
        row.workflowType = microCase.getWorkflowType();
        row.stage = microCase.getStage();
        row.priority = microCase.getPriority();
        row.createdAt = microCase.getCreatedAt();
        row.needsAstReview = needsAstReview(isolates);
        row.hasOpenCriticalCommunication = hasOpenCriticalCommunication(communications);
        row.dueAction = dueAction(microCase, isolates, row.needsAstReview);
        row.urgency = urgency(microCase, row.needsAstReview, row.hasOpenCriticalCommunication);
        for (MicroCase sibling : caseDAO.getBySampleItem(microCase.getSampleItemId())) {
            if (!sibling.getId().equals(microCase.getId())) {
                row.siblingWorkflows.add(sibling.getWorkflowType());
            }
        }
        return row;
    }

    private boolean needsAstReview(List<MicroIsolate> isolates) {
        for (MicroIsolate isolate : isolates) {
            for (MicroAstRun run : astRunDAO.getByIsolateId(isolate.getId())) {
                if (!MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOpenCriticalCommunication(List<MicroCriticalCommunication> communications) {
        for (MicroCriticalCommunication communication : communications) {
            if (MicroCriticalCommunicationStatus.OPEN.name().equals(communication.getAcknowledgementStatus())
                    && Boolean.TRUE.equals(communication.getFollowUpNeeded())) {
                return true;
            }
        }
        return false;
    }

    private String dueAction(MicroCase microCase, List<MicroIsolate> isolates, boolean needsAstReview) {
        if (needsAstReview) {
            return "AST_REVIEW";
        }
        if (MicroCaseStage.RECEIVED.name().equals(microCase.getStage())) {
            return "SETUP";
        }
        if (isolates.isEmpty()) {
            return "ISOLATE_ID";
        }
        for (MicroIsolate isolate : isolates) {
            if (MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())) {
                return "AST_ENTRY";
            }
        }
        return "CASE_REVIEW";
    }

    private String urgency(MicroCase microCase, boolean needsAstReview, boolean hasOpenCriticalCommunication) {
        if (needsAstReview || hasOpenCriticalCommunication || "STAT".equals(microCase.getPriority())
                || "URGENT".equals(microCase.getPriority())) {
            return "HIGH";
        }
        return "ROUTINE";
    }

    private int urgencyRank(MicroWorklistRowForm row) {
        return "HIGH".equals(row.urgency) ? 0 : 1;
    }

    private int actionRank(MicroWorklistRowForm row) {
        if ("AST_REVIEW".equals(row.dueAction)) {
            return 0;
        }
        if ("SETUP".equals(row.dueAction)) {
            return 1;
        }
        if ("ISOLATE_ID".equals(row.dueAction)) {
            return 2;
        }
        if ("AST_ENTRY".equals(row.dueAction)) {
            return 3;
        }
        return 4;
    }
}
