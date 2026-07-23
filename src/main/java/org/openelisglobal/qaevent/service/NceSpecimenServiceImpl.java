package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceSpecimenDAO;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceSpecimenServiceImpl extends AuditableBaseObjectServiceImpl<NceSpecimen, Integer>
        implements NceSpecimenService {

    @Autowired
    protected NceSpecimenDAO baseObjectDAO;

    public NceSpecimenServiceImpl() {
        super(NceSpecimen.class);
        this.auditTrailLog = true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceSpecimen> getSpecimenByNceId(Integer nceId) {
        return baseObjectDAO.getSpecimenByNceId(nceId);
    }

    @Override
    protected NceSpecimenDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceSpecimen> getSpecimenBySampleItemId(Integer sampleId) {
        return baseObjectDAO.getSpecimenBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId) {
        return baseObjectDAO.existsByNceIdAndSampleItemId(nceId, sampleItemId);
    }
}
