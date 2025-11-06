package org.openelisglobal.common.service;

import org.openelisglobal.common.dao.DatabaseChangeLogDAO;
import org.openelisglobal.common.valueholder.DatabaseChangeLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// public class DatabaseChangeLogServiceImpl extends
// AuditableBaseObjectServiceImpl<DatabaseChangeLog, String> implements DatabaseChangeLogService {
public class DatabaseChangeLogServiceImpl implements DatabaseChangeLogService {
    @Autowired
    protected DatabaseChangeLogDAO baseObjectDAO;

    // public DatabaseChangeLogServiceImpl() {
    // super(DatabaseChangeLog.class);
    // }

    // @Override
    protected DatabaseChangeLogDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseChangeLog getLastExecutedChange() {
        return getBaseObjectDAO().getLastExecutedChange();
    }
}
