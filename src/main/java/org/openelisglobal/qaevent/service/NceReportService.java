package org.openelisglobal.qaevent.service;

import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceReportService {

    @PreAuthorize("hasAuthority('PRIV_NCE_CREATE')")
    NcEvent report(NonConformingEventForm form, String authenticatedUserId);
}
