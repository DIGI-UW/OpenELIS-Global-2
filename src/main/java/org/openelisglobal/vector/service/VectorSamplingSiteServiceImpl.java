package org.openelisglobal.vector.service;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.log.LogEvent;
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
    @Transactional(readOnly = true)
    public List<VectorSamplingSite> search(String searchTerm) {
        return getBaseObjectDAO().search(searchTerm);
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

    @Override
    @Transactional
    public String resolveOrCreateForOrder(String siteId, String siteName, String siteCode, String siteType,
            String sysUserId) {
        // Everything below is a self-invocation, so the inherited write gate on
        // insert/update is not re-evaluated: the @PreAuthorize on this method is
        // the check, and it is the one that names order entry.
        if (!GenericValidator.isBlankOrNull(siteId)) {
            try {
                VectorSamplingSite existing = get(Integer.valueOf(siteId));
                boolean changed = false;
                if (!GenericValidator.isBlankOrNull(siteName) && !siteName.equals(existing.getName())) {
                    existing.setName(siteName);
                    changed = true;
                }
                if (!GenericValidator.isBlankOrNull(siteCode) && !siteCode.equals(existing.getCode())) {
                    existing.setCode(siteCode);
                    changed = true;
                }
                if (!GenericValidator.isBlankOrNull(siteType) && !siteType.equals(existing.getType())) {
                    existing.setType(siteType);
                    changed = true;
                }
                if (changed) {
                    existing.setSysUserId(sysUserId);
                    update(existing);
                }
            } catch (NumberFormatException | ObjectNotFoundException e) {
                LogEvent.logError(this.getClass().getName(), "resolveOrCreateForOrder",
                        "Could not update sampling site id=" + siteId + ": " + e.getMessage());
            }
            return siteId;
        }
        if (GenericValidator.isBlankOrNull(siteName) || GenericValidator.isBlankOrNull(siteCode)) {
            return siteId;
        }
        VectorSamplingSite byCode = getByCode(siteCode);
        if (byCode != null) {
            return String.valueOf(byCode.getId());
        }
        VectorSamplingSite newSite = new VectorSamplingSite();
        newSite.setName(siteName);
        newSite.setCode(siteCode);
        newSite.setType(siteType);
        newSite.setActive(true);
        newSite.setSource("LOCAL");
        newSite.setSysUserId(sysUserId);
        return String.valueOf(insert(newSite));
    }
}
