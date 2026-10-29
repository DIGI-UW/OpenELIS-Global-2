package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstReportChoiceDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstReportChoice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstReportChoiceDAOImpl extends BaseDAOImpl<MicroAstReportChoice, String>
        implements MicroAstReportChoiceDAO {
    public MicroAstReportChoiceDAOImpl() {
        super(MicroAstReportChoice.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstReportChoice> getByIsolateId(String id) {
        return entityManager
                .createQuery("from MicroAstReportChoice e where e.isolateId = :id", MicroAstReportChoice.class)
                .setParameter("id", id).getResultList();
    }
}
