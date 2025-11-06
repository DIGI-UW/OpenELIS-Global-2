package org.openelisglobal.datasubmission.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.datasubmission.dao.DataValueDAO;
import org.openelisglobal.datasubmission.valueholder.DataValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DataValueDAOImpl extends BaseDAOImpl<DataValue, String> implements DataValueDAO {

    public DataValueDAOImpl() {
        super(DataValue.class);
    }

    // @Override
    // public void getData(DataValue dataValue) throws LIMSRuntimeException {
    // try {
    // DataValue dataValueClone =
    // entityManager.unwrap(Session.class).get(DataValue.class,
    // dataValue.getId());
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // if (dataValueClone != null) {
    // PropertyUtils.copyProperties(dataValue, dataValueClone);
    // } else {
    // dataValue.setId(null);
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataValueDAOImpl", "getData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataValue getData()", e);
    // }
    // }

    // @Override
    // public DataValue getDataValue(String id) throws LIMSRuntimeException {
    // try {
    // DataValue dataValue =
    // entityManager.unwrap(Session.class).get(DataValue.class, id);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // return dataValue;
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataValueDAOImpl", "getData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataValue getData()", e);
    // }
    // }

    // @Override
    // public boolean insertData(DataValue dataValue) throws LIMSRuntimeException {
    //
    // try {
    // String id = (String) entityManager.unwrap(Session.class).save(dataValue);
    // dataValue.setId(id);
    //
    // String sysUserId = dataValue.getSysUserId();
    // String tableName = "DATA_VALUE";
    // auditDAO.saveNewHistory(dataValue, sysUserId, tableName);
    //
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    //
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataValueDAOImpl", "insertData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataValue insertData()", e);
    // }
    //
    // return true;
    // }

    // @Override
    // public void updateData(DataValue dataValue) throws LIMSRuntimeException {
    //
    // DataValue oldData = getDataValue(dataValue.getId());
    //
    // try {
    //
    // String sysUserId = dataValue.getSysUserId();
    // String event = IActionConstants.AUDIT_TRAIL_UPDATE;
    // String tableName = "DATA_VALUE";
    // // auditDAO.saveHistory(dataValue, oldData, sysUserId, event, tableName);
    // } catch (RuntimeException e) {
    // LogEvent.logError("DataValueDAOImpl", "AuditTrail updateData()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in DataValue AuditTrail updateData()",
    // e);
    // }
    //
    // try {
    // entityManager.unwrap(Session.class).merge(dataValue);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove old(dataValue);
    // // entityManager.unwrap(Session.class).refresh // CSL remove old(dataValue);
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataValueDAOImpl", "updateData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataValue updateData()", e);
    // }
    // }

}
