package org.openelisglobal.qaevent.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.form.InlineNCERequestForm;
import org.openelisglobal.qaevent.form.NCEResponseForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface NCEventService extends BaseObjectService<NcEvent, String> {

    List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId);

    /**
     * Create an NCE from an inline request form. Handles entity population, NCE
     * number generation, and persistence within a transaction.
     *
     * @param request     the inline NCE request form
     * @param resultId    the ID of the associated result
     * @param user        the current user
     * @param contextInfo auto-populated context (lab number, patient info, etc.)
     * @return the saved NcEvent
     */
    NcEvent createNCEFromInlineRequest(InlineNCERequestForm request, String resultId, SystemUser user,
            Map<String, Object> contextInfo);

    /**
     * Build an NCE response form from a saved NcEvent.
     *
     * @param ncEvent           the saved NCE
     * @param associatedResults the list of associated result IDs
     * @return the response form
     */
    NCEResponseForm buildNCEResponse(NcEvent ncEvent, List<String> associatedResults);
}
