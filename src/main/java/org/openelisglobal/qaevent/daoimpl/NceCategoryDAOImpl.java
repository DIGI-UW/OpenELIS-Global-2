package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceCategoryDAO;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceCategoryDAOImpl extends BaseDAOImpl<NceCategory, Integer> implements NceCategoryDAO {

    public NceCategoryDAOImpl() {
        super(NceCategory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceCategory> getAllNceCategory() throws LIMSRuntimeException {
        try {
            String sql = "from NceCategory nc order by nc.id";
            TypedQuery<NceCategory> query = entityManager.createQuery(sql, NceCategory.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceCategory getAllNceCategory()", e);
        }
    }
}
