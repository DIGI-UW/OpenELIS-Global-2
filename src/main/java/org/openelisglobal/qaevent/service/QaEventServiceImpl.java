package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.QaEventDAO;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QaEventServiceImpl extends AuditableBaseObjectServiceImpl<QaEvent, String> implements QaEventService {
    @Autowired
    protected QaEventDAO baseObjectDAO;

    QaEventServiceImpl() {
        super(QaEvent.class);
    }

    @Override
    protected QaEventDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(QaEvent qaEvent) {
        getBaseObjectDAO().getData(qaEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public QaEvent getQaEventByName(QaEvent qaEvent) {
        return getBaseObjectDAO().getQaEventByName(qaEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getQaEvents(String filter) {
        return getBaseObjectDAO().getQaEvents(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getAllQaEvents() {
        return getBaseObjectDAO().getAllQaEvents();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalQaEventCount() {
        return getBaseObjectDAO().getTotalQaEventCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QaEvent> getPageOfQaEvents(int startingRecNo) {
        return getBaseObjectDAO().getPageOfQaEvents(startingRecNo);
    }

    @Override
    // TODO csl confirm that this is correct
    public void delete(QaEvent qaEvent) {
        update(qaEvent, IActionConstants.AUDIT_TRAIL_DELETE);
    }

    @Override
    public String insert(QaEvent qaEvent) {
        if (duplicateQaEventExists(qaEvent)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + qaEvent.getQaEventName());
        }
        return super.insert(qaEvent);
    }

    @Override
    public QaEvent save(QaEvent qaEvent) {
        if (duplicateQaEventExists(qaEvent)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + qaEvent.getQaEventName());
        }
        return super.save(qaEvent);
    }

    @Override
    public QaEvent update(QaEvent qaEvent) {
        if (duplicateQaEventExists(qaEvent)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + qaEvent.getQaEventName());
        }
        return super.update(qaEvent);
    }

    private boolean duplicateQaEventExists(QaEvent qaEvent) {
        return baseObjectDAO.duplicateQaEventExists(qaEvent);
    }
}
