package org.openelisglobal.action.service;

import org.openelisglobal.action.dao.ActionDAO;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionServiceImpl extends AuditableBaseObjectServiceImpl<Action, String> implements ActionService {

    private final ActionDAO baseObjectDAO;

    @Autowired
    public ActionServiceImpl(ActionDAO baseObjectDAO) {
        super(Action.class);
        this.baseObjectDAO = baseObjectDAO;
    }

    @Override
    protected ActionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
