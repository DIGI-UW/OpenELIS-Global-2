package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAntibioticDAOImpl extends BaseDAOImpl<MicroAntibiotic, String> implements MicroAntibioticDAO {

    public MicroAntibioticDAOImpl() {
        super(MicroAntibiotic.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> getActiveAntibiotics() {
        return entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where a.isActive = 'Y' order by a.displayName", MicroAntibiotic.class).list();
    }
}
