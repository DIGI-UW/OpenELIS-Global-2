package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroDstResolutionDAO;
import org.openelisglobal.microbiology.valueholder.MicroDstResolution;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroDstResolutionDAOImpl extends BaseDAOImpl<MicroDstResolution, String>
        implements MicroDstResolutionDAO {
    public MicroDstResolutionDAOImpl() {
        super(MicroDstResolution.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroDstResolution> getByCaseId(String id) {
        return entityManager.createQuery("from MicroDstResolution e where e.caseId = :id", MicroDstResolution.class)
                .setParameter("id", id).getResultList();
    }
}
