package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.reports.vectorsurveillance.manualentry.dao.ManualEntryFieldMapDAO;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualEntryFieldMapServiceImpl extends AuditableBaseObjectServiceImpl<ManualEntryFieldMap, Integer>
        implements ManualEntryFieldMapService {

    @Autowired
    protected ManualEntryFieldMapDAO baseObjectDAO;

    public ManualEntryFieldMapServiceImpl() {
        super(ManualEntryFieldMap.class);
    }

    @Override
    protected ManualEntryFieldMapDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntryFieldMap> getAllOrdered() {
        return getBaseObjectDAO().getAllOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualEntryFieldMap> getVisibleOrdered() {
        return getBaseObjectDAO().getVisibleOrdered();
    }

    @Override
    @Transactional
    public Integer create(ManualEntryFieldMap fieldMap, String sysUserId) {
        if (fieldMap.getVisible() == null) {
            fieldMap.setVisible(Boolean.TRUE);
        }
        fieldMap.setSysUserId(sysUserId);
        return insert(fieldMap);
    }

    @Override
    @Transactional
    public ManualEntryFieldMap patchUpdate(Integer id, ManualEntryFieldMap patch, String sysUserId) {
        ManualEntryFieldMap existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, ManualEntryFieldMap.class.getName()));
        if (patch.getMetricKey() != null) {
            existing.setMetricKey(patch.getMetricKey());
        }
        if (patch.getFieldOrder() != null) {
            existing.setFieldOrder(patch.getFieldOrder());
        }
        if (patch.getLabel() != null) {
            existing.setLabel(patch.getLabel());
        }
        existing.setPortalTag(patch.getPortalTag());
        if (patch.getVisible() != null) {
            existing.setVisible(patch.getVisible());
        }
        existing.setSysUserId(sysUserId);
        return update(existing);
    }
}
