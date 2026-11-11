package org.openelisglobal.qaevent.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceHistoryDAO;
import org.openelisglobal.qaevent.valueholder.NceHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceHistoryServiceImpl extends AuditableBaseObjectServiceImpl<NceHistory, Integer>
        implements NceHistoryService {

    @Autowired
    private NceHistoryDAO nceHistoryDAO;

    public NceHistoryServiceImpl() {
        super(NceHistory.class);
    }

    @Override
    protected NceHistoryDAO getBaseObjectDAO() {
        return nceHistoryDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceHistory> findByNceId(Integer nceId) {
        return nceHistoryDAO.findByNceId(nceId);
    }

    @Override
    @Transactional
    public NceHistory logActivity(Integer nceId, String activity, String description, String oldValue, String newValue,
            Integer userId) {
        NceHistory history = new NceHistory();
        history.setNceId(nceId);
        history.setActivity(activity);
        history.setDescription(description);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setUserId(userId);
        history.setTimestamp(new Timestamp(System.currentTimeMillis()));
        history.setSysUserId(userId != null ? String.valueOf(userId) : null);

        Integer id = insert(history);
        history.setId(id);
        return history;
    }
}
