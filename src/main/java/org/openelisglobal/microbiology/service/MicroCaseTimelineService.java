package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;

public interface MicroCaseTimelineService {

    List<MicroCaseActivityForm> getTimeline(String caseId);

    MicroCaseActivityForm addNote(String caseId, String text, String performedBy);
}
