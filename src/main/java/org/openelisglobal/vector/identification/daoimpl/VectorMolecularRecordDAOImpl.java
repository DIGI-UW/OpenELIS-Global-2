package org.openelisglobal.vector.identification.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.identification.dao.VectorMolecularRecordDAO;
import org.openelisglobal.vector.identification.valueholder.VectorMolecularRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorMolecularRecordDAOImpl extends BaseDAOImpl<VectorMolecularRecord, Long>
        implements VectorMolecularRecordDAO {

    public VectorMolecularRecordDAOImpl() {
        super(VectorMolecularRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VectorMolecularRecord> getByIdentificationId(Long identificationId) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorMolecularRecord> query = entityManager.createQuery(
                    "select m from VectorMolecularRecord m where m.identificationId = :identificationId",
                    VectorMolecularRecord.class);
            query.setParameter("identificationId", identificationId);
            List<VectorMolecularRecord> list = query.getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorMolecularRecordDAOImpl.getByIdentificationId()", e);
        }
    }
}
