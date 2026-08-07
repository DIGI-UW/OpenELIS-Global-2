package org.openelisglobal.result.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.historyservices.AnalysisHistoryService;
import org.openelisglobal.common.services.historyservices.ResultHistoryService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisTimelineServiceImpl implements AnalysisTimelineService {

    private static final String STATUS_ATTRIBUTE = "status";

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private ReferralService referralService;
    @Autowired
    private NceSpecimenService nceSpecimenService;
    @Autowired
    private NCEventService ncEventService;

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisTimelineEvent> getTimeline(Analysis detached) {
        Analysis analysis = analysisService.get(detached.getId());
        List<AnalysisTimelineEvent> events = new ArrayList<>();
        addAnalysisAuditEvents(analysis, events);
        addResultAuditEvents(analysis, events);
        addNoteEvents(analysis, events);
        addRetestEvents(analysis, events);
        addReflexEvents(analysis, events);
        addReferralEvents(analysis, events);
        addNceEvents(analysis, events);
        events.sort(Comparator.comparingLong(AnalysisTimelineEvent::getTimestamp).reversed());
        return events;
    }

    /** OGC-1023 (R4): the test-referred event, from the Referral record itself. */
    private void addReferralEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            Referral referral = referralService.getReferralByAnalysisId(analysis.getId());
            if (referral == null || referral.isCanceled()) {
                return;
            }
            Timestamp when = referral.getSentDate() != null ? referral.getSentDate() : referral.getLastupdated();
            events.add(new AnalysisTimelineEvent("REFERRAL", when != null ? when.getTime() : 0,
                    when != null ? DateUtil.convertTimestampToStringDateAndTime(when) : "",
                    referral.getOrganizationName() != null ? referral.getOrganizationName() : "", ""));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    /** OGC-1023 (R4): non-conformities filed against this analysis. */
    private void addNceEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            if (analysis.getSampleItem() == null) {
                return;
            }
            for (NceSpecimen specimen : nceSpecimenService
                    .getSpecimenBySampleItemId(Integer.valueOf(analysis.getSampleItem().getId()))) {
                if (specimen.getAnalysisId() != null
                        && !String.valueOf(specimen.getAnalysisId()).equals(analysis.getId())) {
                    continue;
                }
                NcEvent nce = ncEventService.get(specimen.getNceId());
                if (nce == null) {
                    continue;
                }
                Timestamp when = nce.getLastupdated();
                events.add(new AnalysisTimelineEvent("NCE", when != null ? when.getTime() : 0,
                        when != null ? DateUtil.convertTimestampToStringDateAndTime(when) : "",
                        (nce.getNceNumber() != null ? nce.getNceNumber() : "")
                                + (nce.getName() != null ? " — " + nce.getName() : ""),
                        nce.getNameOfReporter() != null ? nce.getNameOfReporter() : ""));
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private void addAnalysisAuditEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            for (AuditTrailItem item : new AnalysisHistoryService(analysis).getAuditTrailItems()) {
                if ("I".equals(item.getAction())) {
                    events.add(fromAuditItem(item, "CREATED", ""));
                } else if (STATUS_ATTRIBUTE.equals(item.getAttribute())) {
                    events.add(fromAuditItem(item, "STATUS", transitionDetail(item)));
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private void addResultAuditEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            for (Result result : resultService.getResultsByAnalysis(analysis)) {
                for (AuditTrailItem item : new ResultHistoryService(result, analysis).getAuditTrailItems()) {
                    events.add(fromAuditItem(item, "RESULT", transitionDetail(item)));
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private void addNoteEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            for (Note note : noteService.getNotes(analysis)) {
                Timestamp when = note.getLastupdated();
                String subject = GenericValidator.isBlankOrNull(note.getSubject()) ? "" : note.getSubject() + ": ";
                events.add(new AnalysisTimelineEvent("NOTE", when != null ? when.getTime() : 0,
                        when != null ? DateUtil.convertTimestampToStringDateAndTime(when) : "",
                        subject + note.getText(), noteAuthor(note)));
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    /**
     * Prior revisions of this test on the same sample item — each one is a retest
     * that this analysis superseded.
     */
    private void addRetestEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            for (Analysis revision : analysisService
                    .getRevisionHistoryOfAnalysesBySampleAndTest(analysis.getSampleItem(), analysis.getTest(), false)) {
                Timestamp when = revision.getLastupdated();
                events.add(new AnalysisTimelineEvent("RETEST", when != null ? when.getTime() : 0,
                        when != null ? DateUtil.convertTimestampToStringDateAndTime(when) : "",
                        "revision " + revision.getRevision(), ""));
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    /** Reflex analyses this analysis's results triggered. */
    private void addReflexEvents(Analysis analysis, List<AnalysisTimelineEvent> events) {
        try {
            for (Result result : resultService.getResultsByAnalysis(analysis)) {
                for (Analysis child : analysisService.getAllChildAnalysesByResult(result)) {
                    Timestamp when = child.getEnteredDate() != null ? child.getEnteredDate() : child.getLastupdated();
                    String childTest = child.getTest() != null
                            ? TestServiceImpl.getLocalizedTestNameWithType(child.getTest())
                            : "";
                    events.add(new AnalysisTimelineEvent("REFLEX", when != null ? when.getTime() : 0,
                            when != null ? DateUtil.convertTimestampToStringDateAndTime(when) : "", childTest, ""));
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private AnalysisTimelineEvent fromAuditItem(AuditTrailItem item, String type, String detail) {
        long timestamp = item.getTimeStamp() != null ? item.getTimeStamp().getTime() : 0;
        String when = item.getTimeStamp() != null ? DateUtil.convertTimestampToStringDateAndTime(item.getTimeStamp())
                : "";
        return new AnalysisTimelineEvent(type, timestamp, when, detail, item.getUser() != null ? item.getUser() : "");
    }

    private String transitionDetail(AuditTrailItem item) {
        boolean hasOld = !GenericValidator.isBlankOrNull(item.getOldValue());
        boolean hasNew = !GenericValidator.isBlankOrNull(item.getNewValue());
        if (hasOld && hasNew) {
            return item.getOldValue() + " → " + item.getNewValue();
        }
        return hasNew ? item.getNewValue() : (hasOld ? item.getOldValue() : "");
    }

    private String noteAuthor(Note note) {
        if (note.getSystemUser() == null || GenericValidator.isBlankOrNull(note.getSystemUser().getId())) {
            return "";
        }
        SystemUser author = systemUserService.get(note.getSystemUser().getId());
        return author != null ? author.getDisplayName() : "";
    }
}
