package org.openelisglobal.common.service;

import org.openelisglobal.common.valueholder.DatabaseChangeLog;

// public interface DatabaseChangeLogService extends BaseObjectService<DatabaseChangeLog, String> {

public interface DatabaseChangeLogService {
    DatabaseChangeLog getLastExecutedChange();
}
