package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorTrapTypeDAO;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorTrapTypeServiceImpl extends AuditableBaseObjectServiceImpl<VectorTrapType, String>
        implements VectorTrapTypeService {

    @Autowired
    protected VectorTrapTypeDAO baseObjectDAO;

    public VectorTrapTypeServiceImpl() {
        super(VectorTrapType.class);
    }

    @Override
    protected VectorTrapTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorTrapType> getByGroupId(String groupId) {
        return getBaseObjectDAO().getByGroupId(groupId);
    }
}
