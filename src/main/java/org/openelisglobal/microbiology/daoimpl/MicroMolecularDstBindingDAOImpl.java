package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroMolecularDstBindingDAO;
import org.openelisglobal.microbiology.valueholder.MicroMolecularDstBinding;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroMolecularDstBindingDAOImpl extends BaseDAOImpl<MicroMolecularDstBinding, String>
        implements MicroMolecularDstBindingDAO {
    public MicroMolecularDstBindingDAOImpl() {
        super(MicroMolecularDstBinding.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroMolecularDstBinding> getBindings() {
        return entityManager.createQuery("from MicroMolecularDstBinding e", MicroMolecularDstBinding.class)
                .getResultList();
    }
}
