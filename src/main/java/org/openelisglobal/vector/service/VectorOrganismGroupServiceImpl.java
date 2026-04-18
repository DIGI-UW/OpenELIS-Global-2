package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorOrganismGroupDAO;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorOrganismGroupServiceImpl extends AuditableBaseObjectServiceImpl<VectorOrganismGroup, String>
        implements VectorOrganismGroupService {

    @Autowired
    protected VectorOrganismGroupDAO baseObjectDAO;

    public VectorOrganismGroupServiceImpl() {
        super(VectorOrganismGroup.class);
    }

    @Override
    protected VectorOrganismGroupDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorOrganismGroup> getActiveGroups() {
        return getBaseObjectDAO().getActiveGroups();
    }

    @Override
    @Transactional(readOnly = true)
    public VectorOrganismGroup getByCode(String code) {
        return getBaseObjectDAO().getByCode(code);
    }
}
