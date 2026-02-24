package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.form.PromptDismissalRequestForm;
import org.openelisglobal.qaevent.valueholder.NcePromptDismissal;

/**
 * Service interface for prompt dismissal audit records.
 */
public interface NcePromptDismissalService extends BaseObjectService<NcePromptDismissal, Integer> {

    NcePromptDismissal recordDismissal(PromptDismissalRequestForm request, String dismissedBy);

    List<NcePromptDismissal> getDismissalsForResult(String resultId);
}
