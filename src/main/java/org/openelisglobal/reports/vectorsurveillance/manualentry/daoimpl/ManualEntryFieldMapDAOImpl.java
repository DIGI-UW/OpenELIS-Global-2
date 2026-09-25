package org.openelisglobal.reports.vectorsurveillance.manualentry.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.vectorsurveillance.manualentry.dao.ManualEntryFieldMapDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ManualEntryFieldMapDAOImpl extends BaseDAOImpl<ManualEntryFieldMap, Integer>
        implements ManualEntryFieldMapDAO {

    public ManualEntryFieldMapDAOImpl() {
        super(ManualEntryFieldMap.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntryFieldMap> getAllOrdered() {
        try {
            TypedQuery<ManualEntryFieldMap> query = entityManager
                    .createQuery("from ManualEntryFieldMap f order by f.fieldOrder", ManualEntryFieldMap.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ManualEntryFieldMapDAOImpl.getAllOrdered()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntryFieldMap> getVisibleOrdered() {
        try {
            TypedQuery<ManualEntryFieldMap> query = entityManager.createQuery(
                    "from ManualEntryFieldMap f where f.visible = true order by f.fieldOrder",
                    ManualEntryFieldMap.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in ManualEntryFieldMapDAOImpl.getVisibleOrdered()", e);
        }
    }
}
