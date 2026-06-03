package org.openelisglobal.vector.identification.service;

import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.vector.identification.dao.VectorMolecularRecordDAO;
import org.openelisglobal.vector.identification.valueholder.VectorMolecularRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorMolecularRecordServiceImpl extends AuditableBaseObjectServiceImpl<VectorMolecularRecord, Long>
        implements VectorMolecularRecordService {

    @Autowired
    protected VectorMolecularRecordDAO baseObjectDAO;

    public VectorMolecularRecordServiceImpl() {
        super(VectorMolecularRecord.class);
    }

    @Override
    protected VectorMolecularRecordDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VectorMolecularRecord> getByIdentificationId(Long identificationId) {
        return getBaseObjectDAO().getByIdentificationId(identificationId);
    }
}
