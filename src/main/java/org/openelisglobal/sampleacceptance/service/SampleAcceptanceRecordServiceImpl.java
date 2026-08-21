package org.openelisglobal.sampleacceptance.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.dao.SampleAcceptanceRecordDAO;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord.Answer;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.service.VectorPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleAcceptanceRecordServiceImpl extends BaseObjectServiceImpl<SampleAcceptanceRecord, Integer>
        implements SampleAcceptanceRecordService {

    @Autowired
    private SampleAcceptanceRecordDAO sampleAcceptanceRecordDAO;

    @Autowired
    private SampleAcceptanceChecklistService checklistService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private VectorPoolService vectorPoolService;

    public SampleAcceptanceRecordServiceImpl() {
        super(SampleAcceptanceRecord.class);
    }

    @Override
    protected BaseDAO<SampleAcceptanceRecord, Integer> getBaseObjectDAO() {
        return sampleAcceptanceRecordDAO;
    }

    @Override
    @Transactional
    public SampleAcceptanceRecord recordAssessment(String sampleItemId, List<Answer> answers, Integer userId) {
        Integer siid = parseId(sampleItemId);
        if (siid == null) {
            throw new IllegalArgumentException("sampleItemId must be numeric: " + sampleItemId);
        }
        String domain = resolveDomain(sampleItemId);
        List<Dictionary> items = checklistService.listForDomain(domain);
        List<Answer> safeAnswers = answers != null ? answers : new ArrayList<>();

        SampleAcceptanceRecord record = new SampleAcceptanceRecord();
        record.setSampleItemId(siid);
        record.setDomain(domain);
        record.setAnswers(safeAnswers);
        record.setOverallStatus(computeStatus(items, safeAnswers));
        record.setAssessedByUserId(userId);
        if (userId != null) {
            record.setSysUserId(String.valueOf(userId));
        }
        // Append-only: a fresh entity (null id) always inserts a new row.
        return save(record);
    }

    @Override
    @Transactional
    public List<SampleAcceptanceRecord> recordAssessmentForPool(Integer vectorPoolId, List<Answer> answers,
            Integer userId) {
        if (vectorPoolId == null) {
            throw new IllegalArgumentException("vectorPoolId must not be null");
        }
        List<SampleItem> members = vectorPoolService.getMembersByPoolId(vectorPoolId);
        if (members.isEmpty()) {
            throw new IllegalArgumentException("No members for vector pool " + vectorPoolId);
        }
        // The pool is the unit of acceptance; cascade the same assessment to each
        // live member so the per-specimen gate (evaluateOrder) needs no change.
        List<SampleAcceptanceRecord> records = new ArrayList<>();
        for (SampleItem member : members) {
            if (member.isVoided() || member.isRejected()) {
                continue;
            }
            records.add(recordAssessment(member.getId(), answers, userId));
        }
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public SampleAcceptanceRecord getLatest(String sampleItemId) {
        return sampleAcceptanceRecordDAO.findLatestBySampleItemId(parseId(sampleItemId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleAcceptanceRecord> getHistory(String sampleItemId) {
        return sampleAcceptanceRecordDAO.findHistoryBySampleItemId(parseId(sampleItemId));
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveDomain(String sampleItemId) {
        Integer siid = parseId(sampleItemId);
        if (siid == null) {
            return null;
        }
        SampleItem item = sampleItemService.get(String.valueOf(siid));
        if (item == null) {
            return null;
        }
        Sample sample = item.getSample();
        return sample == null ? null : mapDomain(sample.getDomain());
    }

    @Override
    @Transactional(readOnly = true)
    public SampleAcceptanceEvaluation evaluate(String sampleItemId) {
        String domain = resolveDomain(sampleItemId);
        String enforcement = checklistService.getEnforcement(domain);
        List<Dictionary> items = checklistService.listForDomain(domain);
        SampleAcceptanceRecord latest = getLatest(sampleItemId);

        String status;
        if (latest != null) {
            status = latest.getOverallStatus();
        } else {
            // An empty resolved checklist is ACCEPTED (nothing to assess). This is
            // intentional fail-open: even under MANDATORY enforcement a domain with no
            // configured items does not block — no checklist means no gate.
            status = items.isEmpty() ? SampleAcceptanceRecord.STATUS_ACCEPTED : SampleAcceptanceRecord.STATUS_PENDING;
        }
        boolean blocked = "MANDATORY".equalsIgnoreCase(enforcement)
                && !SampleAcceptanceRecord.STATUS_ACCEPTED.equals(status);

        SampleAcceptanceEvaluation eval = new SampleAcceptanceEvaluation();
        eval.setSampleItemId(sampleItemId);
        eval.setDomain(domain);
        eval.setEnforcement(enforcement);
        eval.setItems(items);
        eval.setLatest(latest);
        eval.setOverallStatus(status);
        eval.setBlocked(blocked);
        return eval;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleAcceptanceEvaluation> evaluateOrder(String sampleId) {
        List<SampleAcceptanceEvaluation> evaluations = new ArrayList<>();
        if (parseId(sampleId) == null) {
            return evaluations;
        }
        for (SampleItem item : sampleItemService.getSampleItemsBySampleId(sampleId)) {
            // Skip resolved specimens (voided, or rejected/resampled away) — they have
            // left the workflow and must not appear as an open acceptance row.
            if (item.isVoided() || item.isRejected()) {
                continue;
            }
            evaluations.add(evaluate(item.getId()));
        }
        return evaluations;
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceAcceptanceGate(String sampleItemId) {
        SampleAcceptanceEvaluation eval = evaluate(sampleItemId);
        if (eval.isBlocked()) {
            throw new SampleAcceptanceBlockedException("Specimen " + sampleItemId + " cannot proceed: the "
                    + eval.getDomain() + " acceptance checklist is MANDATORY and not yet satisfied (status "
                    + eval.getOverallStatus() + ").");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceAcceptanceGateForOrder(String sampleId) {
        if (parseId(sampleId) == null) {
            return;
        }
        for (SampleItem item : sampleItemService.getSampleItemsBySampleId(sampleId)) {
            // A voided or rejected specimen is already resolved (removed, rejected, or
            // resampled away) and does not block the order from advancing.
            if (item.isVoided() || item.isRejected()) {
                continue;
            }
            enforceAcceptanceGate(item.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String buildNcePrefillReason(String sampleItemId) {
        SampleAcceptanceRecord latest = getLatest(sampleItemId);
        if (latest == null) {
            return null;
        }
        List<String> fails = new ArrayList<>();
        for (Answer a : latest.getAnswers()) {
            if (SampleAcceptanceRecord.ANSWER_FAIL.equals(a.getAnswer())) {
                String label = (a.getLabel() != null && !a.getLabel().isBlank()) ? a.getLabel() : a.getItemKey();
                if (a.getNote() != null && !a.getNote().isBlank()) {
                    fails.add(label + " (" + a.getNote().trim() + ")");
                } else {
                    fails.add(label);
                }
            }
        }
        return fails.isEmpty() ? null : "Sample acceptance failures: " + String.join("; ", fails);
    }

    /**
     * ACCEPTED when every resolved item is answered PASS/NA; REVIEW when any item
     * is FAIL; PENDING when some resolved item is unanswered. An empty checklist is
     * ACCEPTED (nothing to assess — never blocks).
     */
    private String computeStatus(List<Dictionary> items, List<Answer> answers) {
        if (items == null || items.isEmpty()) {
            return SampleAcceptanceRecord.STATUS_ACCEPTED;
        }
        Map<String, String> answerByKey = new HashMap<>();
        for (Answer a : answers) {
            if (a.getItemKey() != null) {
                answerByKey.put(a.getItemKey(), a.getAnswer());
            }
        }
        boolean anyFail = false;
        boolean allAnswered = true;
        for (Dictionary item : items) {
            String ans = answerByKey.get(item.getDictEntry());
            if (SampleAcceptanceRecord.ANSWER_FAIL.equals(ans)) {
                anyFail = true;
            }
            if (ans == null || ans.isBlank()) {
                allAnswered = false;
            }
        }
        if (anyFail) {
            return SampleAcceptanceRecord.STATUS_REVIEW;
        }
        return allAnswered ? SampleAcceptanceRecord.STATUS_ACCEPTED : SampleAcceptanceRecord.STATUS_PENDING;
    }

    /**
     * Map the parent sample's stored domain code ({@code sample.domain} is H/E/V)
     * to the full-word domain used by the checklist categories. Passes through full
     * words, returns null for unknown/blank (= lab-wide).
     */
    private String mapDomain(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        switch (code.trim().toUpperCase()) {
        case "H":
        case "CLINICAL":
            return "CLINICAL";
        case "E":
        case "ENVIRONMENTAL":
            return "ENVIRONMENTAL";
        case "V":
        case "VECTOR":
            return "VECTOR";
        default:
            return null;
        }
    }

    private Integer parseId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
