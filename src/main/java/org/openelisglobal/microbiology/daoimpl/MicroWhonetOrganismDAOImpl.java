package org.openelisglobal.microbiology.daoimpl;

import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetOrganism;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWhonetOrganismDAOImpl extends BaseDAOImpl<MicroWhonetOrganism, String>
        implements MicroWhonetOrganismDAO {

    public MicroWhonetOrganismDAOImpl() {
        super(MicroWhonetOrganism.class);
    }

    @Override
    public void deleteAllRows() {
        entityManager.createQuery("delete from MicroWhonetOrganism").executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroWhonetOrganism> findCurrentByCode(String whonetOrgCode) {
        return entityManager
                .createQuery("from MicroWhonetOrganism w where w.taxonomicStatus = 'C'"
                        + " and lower(w.whonetOrgCode) = lower(:code)", MicroWhonetOrganism.class)
                .setParameter("code", whonetOrgCode).setMaxResults(1).getResultStream().findFirst();
    }
}
