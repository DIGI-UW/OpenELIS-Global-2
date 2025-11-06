package org.openelisglobal.observationhistorytype.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.observationhistorytype.dao.ObservationHistoryTypeDAO;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ObservationHistoryTypeDAOImpl extends BaseDAOImpl<ObservationHistoryType, String>
        implements ObservationHistoryTypeDAO {

    public ObservationHistoryTypeDAOImpl() {
        super(ObservationHistoryType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ObservationHistoryType getByName(String name) throws LIMSRuntimeException {
        List<ObservationHistoryType> historyTypeList;

        try {
            String sql = "from ObservationHistoryType oht where oht.typeName = :name";
            Query<ObservationHistoryType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ObservationHistoryType.class);
            query.setParameter("name", name);
            historyTypeList = query.list();

            return historyTypeList.size() > 0 ? historyTypeList.get(0) : null;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ObservationHistoryTypeDAOImpl  getByName()", e);
        }
    }

    /** Read all entities from the database. */
    @Override
    @Transactional(readOnly = true)
    public List<ObservationHistoryType> getAll() throws LIMSRuntimeException {
        List<ObservationHistoryType> entities;
        try {
            String sql = "from ObservationHistoryType";
            Query<ObservationHistoryType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ObservationHistoryType.class);
            entities = query.list();
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
            throw (e);
        }

        return entities;
    }
}
