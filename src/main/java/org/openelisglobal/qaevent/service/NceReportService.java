package org.openelisglobal.qaevent.service;

import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;

public interface NceReportService {

    NcEvent report(NonConformingEventForm form, String authenticatedUserId);
}
