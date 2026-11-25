package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;

public interface MicroCaseTimelineService {

    List<MicroCaseActivityForm> getTimeline(String caseId);

    MicroCaseActivityForm addNote(String caseId, String text, String performedBy);

    MicroCaseActivityForm addNote(String caseId, String text, boolean sendWithResult, String performedBy);

    MicroCaseActivityForm addNote(String caseId, String text, boolean sendWithResult, String targetType,
            String targetId, String performedBy);

    List<MicroCaseActivityForm> getReportNotes(String caseId);
}
