package org.openelisglobal.vector.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.dao.DictionaryDAO;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.vector.dao.VectorOrganismGroupDAO;
import org.openelisglobal.vector.dao.VectorSpeciesDAO;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSpeciesServiceImpl extends AuditableBaseObjectServiceImpl<VectorSpecies, Integer>
        implements VectorSpeciesService {

    @Autowired
    protected VectorSpeciesDAO baseObjectDAO;

    @Autowired
    private VectorOrganismGroupDAO vectorOrganismGroupDAO;

    @Autowired
    private DictionaryDAO dictionaryDAO;

    public VectorSpeciesServiceImpl() {
        super(VectorSpecies.class);
    }

    @Override
    protected VectorSpeciesDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecies> getByGroupId(Integer groupId) {
        return getBaseObjectDAO().getByGroupId(groupId);
    }

    @Override
    @Transactional
    public Integer create(VectorSpecies species, Integer groupId, String sysUserId) {
        if (groupId != null) {
            vectorOrganismGroupDAO.get(groupId).ifPresent(species::setGroup);
        }
        species.setPathogensOfInterest(resolveDict(species.getPathogensOfInterest()));
        species.setLifecycleStages(resolveDict(species.getLifecycleStages()));
        species.setSysUserId(sysUserId);
        return getBaseObjectDAO().insert(species);
    }

    @Override
    @Transactional
    public VectorSpecies patchUpdate(Integer id, VectorSpecies patch, Integer groupId, String sysUserId) {
        VectorSpecies existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorSpecies.class.getName()));
        existing.setGenus(patch.getGenus());
        existing.setSpecies(patch.getSpecies());
        existing.setSubspecies(patch.getSubspecies());
        existing.setPathogensOfInterest(resolveDict(patch.getPathogensOfInterest()));
        existing.setLifecycleStages(resolveDict(patch.getLifecycleStages()));
        if (patch.getActive() != null) {
            existing.setActive(patch.getActive());
        }
        if (groupId != null) {
            vectorOrganismGroupDAO.get(groupId).ifPresent(existing::setGroup);
        }
        existing.setSysUserId(sysUserId);
        return getBaseObjectDAO().update(existing);
    }

    private Set<Dictionary> resolveDict(java.util.Collection<Dictionary> stubs) {
        if (stubs == null || stubs.isEmpty()) {
            return new HashSet<>();
        }
        Set<Dictionary> resolved = new HashSet<>();
        for (Dictionary stub : stubs) {
            if (stub.getId() != null) {
                dictionaryDAO.get(stub.getId()).ifPresent(resolved::add);
            }
        }
        return resolved;
    }
}
