package org.openelisglobal.vector.service;

import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorOrganismGroupDAO;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorOrganismGroupServiceImpl extends AuditableBaseObjectServiceImpl<VectorOrganismGroup, Integer>
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

    @Override
    @Transactional
    public VectorOrganismGroup patchUpdate(Integer id, VectorOrganismGroup patch, String sysUserId) {
        VectorOrganismGroup existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorOrganismGroup.class.getName()));
        existing.setLabel(patch.getLabel());
        existing.setColorTag(patch.getColorTag());
        existing.setDescription(patch.getDescription());
        if (!Boolean.TRUE.equals(existing.getIsSystem())) {
            existing.setCode(patch.getCode());
        }
        existing.setSysUserId(sysUserId);
        return getBaseObjectDAO().update(existing);
    }
}
