package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroOrganismDAOImpl extends BaseDAOImpl<MicroOrganism, String> implements MicroOrganismDAO {

    public MicroOrganismDAOImpl() {
        super(MicroOrganism.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getActiveOrganisms() {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o where o.isActive = 'Y' order by o.displayName", MicroOrganism.class)
                .list();
    }
}
