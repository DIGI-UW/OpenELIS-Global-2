package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NceSpecimenDAO;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NceSpecimenServiceImpl extends AuditableBaseObjectServiceImpl<NceSpecimen, String>
        implements NceSpecimenService {

    @Autowired
    protected NceSpecimenDAO baseObjectDAO;

    public NceSpecimenServiceImpl() {
        super(NceSpecimen.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceSpecimen> getSpecimenByNceId(String nceId) {
        return baseObjectDAO.getSpecimenByNceId(nceId);
    }

    @Override
    protected NceSpecimenDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceSpecimen> getSpecimenBySampleItemId(String sampleId) {
        return baseObjectDAO.getSpecimenBySampleId(sampleId);
    }
}
