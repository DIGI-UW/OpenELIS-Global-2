package org.openelisglobal.vector.service;

import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.dao.VectorSamplingSiteDAO;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSamplingSiteServiceImpl extends AuditableBaseObjectServiceImpl<VectorSamplingSite, Integer>
        implements VectorSamplingSiteService {

    @Autowired
    protected VectorSamplingSiteDAO baseObjectDAO;

    public VectorSamplingSiteServiceImpl() {
        super(VectorSamplingSite.class);
    }

    @Override
    protected VectorSamplingSiteDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSamplingSite> getByType(String type) {
        return getBaseObjectDAO().getByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSamplingSite> getActive() {
        return getBaseObjectDAO().getActive();
    }

    @Override
    @Transactional(readOnly = true)
    public VectorSamplingSite getByCode(String code) {
        return getBaseObjectDAO().getByCode(code);
    }

    @Override
    @Transactional
    public VectorSamplingSite patchUpdate(Integer id, VectorSamplingSite patch, String sysUserId) {
        VectorSamplingSite existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorSamplingSite.class.getName()));
        if (patch.getCode() != null)
            existing.setCode(patch.getCode());
        if (patch.getName() != null)
            existing.setName(patch.getName());
        existing.setType(patch.getType());
        existing.setContactName(patch.getContactName());
        existing.setContactPhone(patch.getContactPhone());
        existing.setGpsLatitude(patch.getGpsLatitude());
        existing.setGpsLongitude(patch.getGpsLongitude());
        existing.setEnvironmentalZone(patch.getEnvironmentalZone());
        existing.setDescription(patch.getDescription());
        existing.setSubtype(patch.getSubtype());
        existing.setLocationOrgId(patch.getLocationOrgId());
        if (patch.getSource() != null)
            existing.setSource(patch.getSource());
        if (patch.getActive() != null)
            existing.setActive(patch.getActive());
        existing.setSysUserId(sysUserId);
        return getBaseObjectDAO().update(existing);
    }
}
