package org.openelisglobal.vector.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorOrganismGroupDAO;
import org.openelisglobal.vector.dao.VectorTrapTypeDAO;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorTrapTypeServiceImpl extends AuditableBaseObjectServiceImpl<VectorTrapType, Integer>
        implements VectorTrapTypeService {

    @Autowired
    protected VectorTrapTypeDAO baseObjectDAO;

    @Autowired
    private VectorOrganismGroupDAO vectorOrganismGroupDAO;

    public VectorTrapTypeServiceImpl() {
        super(VectorTrapType.class);
    }

    @Override
    protected VectorTrapTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorTrapType> getByGroupId(Integer groupId) {
        return getBaseObjectDAO().getByGroupId(groupId);
    }

    @Override
    @Transactional
    public Integer create(VectorTrapType trapType, Set<Integer> groupIds, String sysUserId) {
        trapType.setGroups(resolveGroups(groupIds));
        trapType.setSysUserId(sysUserId);
        return getBaseObjectDAO().insert(trapType);
    }

    @Override
    @Transactional
    public VectorTrapType patchUpdate(Integer id, VectorTrapType patch, Set<Integer> groupIds, String sysUserId) {
        VectorTrapType existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorTrapType.class.getName()));
        existing.setName(patch.getName());
        existing.setDescription(patch.getDescription());
        if (patch.getActive() != null) {
            existing.setActive(patch.getActive());
        }
        if (groupIds != null) {
            existing.setGroups(resolveGroups(groupIds));
        }
        existing.setSysUserId(sysUserId);
        return getBaseObjectDAO().update(existing);
    }

    private Set<VectorOrganismGroup> resolveGroups(Set<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<VectorOrganismGroup> resolved = new HashSet<>();
        for (Integer gid : groupIds) {
            vectorOrganismGroupDAO.get(gid).ifPresent(resolved::add);
        }
        return resolved;
    }
}
