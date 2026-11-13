package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroMolecularDstBinding;

public interface MicroMolecularDstBindingDAO extends BaseDAO<MicroMolecularDstBinding, String> {
    List<MicroMolecularDstBinding> getBindings();
}
