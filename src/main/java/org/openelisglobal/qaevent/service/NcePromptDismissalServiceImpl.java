package org.openelisglobal.qaevent.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NcePromptDismissalDAO;
import org.openelisglobal.qaevent.form.PromptDismissalRequestForm;
import org.openelisglobal.qaevent.valueholder.NcePromptDismissal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NcePromptDismissalServiceImpl extends AuditableBaseObjectServiceImpl<NcePromptDismissal, Integer>
        implements NcePromptDismissalService {

    @Autowired
    protected NcePromptDismissalDAO baseObjectDAO;

    public NcePromptDismissalServiceImpl() {
        super(NcePromptDismissal.class);
    }

    @Override
    protected NcePromptDismissalDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public NcePromptDismissal recordDismissal(PromptDismissalRequestForm request, String dismissedBy) {
        if (request.getTriggerAction() == null || request.getTriggerAction().isBlank()) {
            throw new IllegalArgumentException("Trigger action is required");
        }
        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new IllegalArgumentException("Source type is required");
        }

        NcePromptDismissal dismissal = new NcePromptDismissal();
        dismissal.setTriggerAction(request.getTriggerAction());
        dismissal.setSourceType(request.getSourceType());
        dismissal.setResultId(request.getResultId());
        dismissal.setContext(request.getContext());
        dismissal.setDismissedBy(dismissedBy);
        dismissal.setDismissedDate(new Timestamp(System.currentTimeMillis()));
        dismissal.setSysUserId(dismissedBy);

        return save(dismissal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NcePromptDismissal> getDismissalsForResult(String resultId) {
        return baseObjectDAO.getByResultId(resultId);
    }
}
