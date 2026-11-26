package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWhonetAntibioticDAOImpl extends BaseDAOImpl<MicroWhonetAntibiotic, String>
        implements MicroWhonetAntibioticDAO {

    public MicroWhonetAntibioticDAOImpl() {
        super(MicroWhonetAntibiotic.class);
    }

    @Override
    public void deleteAllRows() {
        entityManager.createQuery("delete from MicroWhonetAntibiotic").executeUpdate();
    }
}
