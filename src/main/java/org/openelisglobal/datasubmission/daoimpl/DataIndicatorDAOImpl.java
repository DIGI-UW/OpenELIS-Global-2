package org.openelisglobal.datasubmission.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.datasubmission.dao.DataIndicatorDAO;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DataIndicatorDAOImpl extends BaseDAOImpl<DataIndicator, String> implements DataIndicatorDAO {

    public DataIndicatorDAOImpl() {
        super(DataIndicator.class);
    }

    // @Override
    // public void getData(DataIndicator indicator) throws LIMSRuntimeException {
    // try {
    // DataIndicator indicatorClone =
    // entityManager.unwrap(Session.class).get(DataIndicator.class,
    // indicator.getId());
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // if (indicatorClone != null) {
    // PropertyUtils.copyProperties(indicator, indicatorClone);
    // } else {
    // indicator.setId(null);
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataIndicatorDAOImpl", "getData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator getData()", e);
    // }
    // }

    // @Override
    // public DataIndicator getIndicator(String id) throws LIMSRuntimeException {
    // try {
    // DataIndicator indicator =
    // entityManager.unwrap(Session.class).get(DataIndicator.class, id);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // return indicator;
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataIndicatorDAOImpl", "getData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator getData()", e);
    // }
    // }

    // @Override
    // public DataIndicator getIndicatorByTypeYearMonth(TypeOfDataIndicator type,
    // int year, int month)
    // throws LIMSRuntimeException {
    // String sql = "From DataIndicator di where di.typeOfIndicator.id = :todiid and
    // di.year = :year
    // and di.month = :month";
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setInteger("todiid", Integer.parseInt(type.getId()));
    // query.setInteger("year", year);
    // query.setInteger("month", month);
    // DataIndicator indicator = (DataIndicator) query.uniqueResult();
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // if (indicator == null) {
    // return null;
    // }
    // return indicator;
    // } catch (HibernateException e) {
    // LogEvent.logError("DataIndicatorDAOImpl", "getIndicatorByTypeYearMonth()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator
    // getIndicatorByTypeYearMonth()", e);
    // }
    // }

    // @Override
    // public List<DataIndicator> getIndicatorsByStatus(String status) throws
    // LIMSRuntimeException {
    // String sql = "From DataIndicator di where di.status = :status";
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setString("status", status);
    // List<DataIndicator> indicators = query.list();
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // return indicators;
    // } catch (HibernateException e) {
    // LogEvent.logError("DataIndicatorDAOImpl", "getIndicatorByStatus()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator
    // getIndicatorByStatus()", e);
    // }
    // }

    // @Override
    // public boolean insertData(DataIndicator dataIndicator) throws
    // LIMSRuntimeException {
    // try {
    // String id = (String) entityManager.unwrap(Session.class).save(dataIndicator);
    // dataIndicator.setId(id);
    //
    // String sysUserId = dataIndicator.getSysUserId();
    // String tableName = "DATA_INDICATOR";
    // // auditDAO.saveNewHistory(dataIndicator, sysUserId, tableName);
    //
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    //
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataIndicatorDAOImpl", "insertData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator insertData()", e);
    // }
    //
    // return true;
    // }

    // @Override
    // public void updateData(DataIndicator dataIndicator) throws
    // LIMSRuntimeException {
    //
    // DataIndicator oldData = getIndicator(dataIndicator.getId());
    //
    // try {
    //
    // String sysUserId = dataIndicator.getSysUserId();
    // String event = IActionConstants.AUDIT_TRAIL_UPDATE;
    // String tableName = "DATA_INDICATOR";
    // // auditDAO.saveHistory(dataIndicator, oldData, sysUserId, event, tableName);
    // } catch (RuntimeException e) {
    // LogEvent.logError("DataIndicatorDAOImpl", "AuditTrail updateData()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator AuditTrail
    // updateData()", e);
    // }
    //
    // try {
    // entityManager.unwrap(Session.class).merge(dataIndicator);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove
    // old(dataIndicator);
    // // entityManager.unwrap(Session.class).refresh // CSL remove
    // old(dataIndicator);
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("DataIndicatorDAOImpl", "updateData()", e.toString());
    // throw new LIMSRuntimeException("Error in DataIndicator updateData()", e);
    // }
    // }

}
