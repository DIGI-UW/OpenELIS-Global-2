package org.openelisglobal.label.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.label.dao.LabelDAO;
import org.openelisglobal.label.valueholder.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LabelServiceImpl extends AuditableBaseObjectServiceImpl<Label, String> implements LabelService {
    @Autowired
    protected LabelDAO baseObjectDAO;

    LabelServiceImpl() {
        super(Label.class);
    }

    @Override
    protected LabelDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
