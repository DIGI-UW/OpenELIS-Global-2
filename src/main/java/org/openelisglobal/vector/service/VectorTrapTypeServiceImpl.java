package org.openelisglobal.vector.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.dao.VectorTrapTypeDAO;
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
    private TypeOfSampleService typeOfSampleService;

    public VectorTrapTypeServiceImpl() {
        super(VectorTrapType.class);
    }

    @Override
    protected VectorTrapTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorTrapType> getBySampleTypeId(String sampleTypeId) {
        List<VectorTrapType> list = getBaseObjectDAO().getBySampleTypeId(sampleTypeId);
        list.forEach(this::enrichSampleTypes);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorTrapType> getAll() {
        List<VectorTrapType> list = super.getAll();
        list.forEach(this::enrichSampleTypes);
        return list;
    }

    @Override
    @Transactional
    public Integer create(VectorTrapType trapType, Set<String> sampleTypeIds, String sysUserId) {
        trapType.setSampleTypeIds(toLongIds(sampleTypeIds));
        trapType.setSysUserId(sysUserId);
        return getBaseObjectDAO().insert(trapType);
    }

    @Override
    @Transactional
    public VectorTrapType patchUpdate(Integer id, VectorTrapType patch, Set<String> sampleTypeIds, String sysUserId) {
        VectorTrapType existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorTrapType.class.getName()));
        existing.setName(patch.getName());
        existing.setDescription(patch.getDescription());
        if (patch.getActive() != null) {
            existing.setActive(patch.getActive());
        }
        if (sampleTypeIds != null) {
            existing.setSampleTypeIds(toLongIds(sampleTypeIds));
        }
        existing.setSysUserId(sysUserId);
        VectorTrapType updated = getBaseObjectDAO().update(existing);
        enrichSampleTypes(updated);
        return updated;
    }

    private void enrichSampleTypes(VectorTrapType t) {
        if (t.getSampleTypeIds() != null && !t.getSampleTypeIds().isEmpty()) {
            List<TypeOfSample> resolved = new ArrayList<>();
            for (Long stId : t.getSampleTypeIds()) {
                TypeOfSample tos = typeOfSampleService.getTypeOfSampleById(String.valueOf(stId));
                if (tos != null) {
                    resolved.add(tos);
                }
            }
            t.setSampleTypes(resolved);
        }
    }

    private Set<Long> toLongIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> result = new HashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                result.add(Long.valueOf(id));
            }
        }
        return result;
    }
}
