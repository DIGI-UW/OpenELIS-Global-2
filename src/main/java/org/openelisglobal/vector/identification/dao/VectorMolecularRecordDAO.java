package org.openelisglobal.vector.identification.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.identification.valueholder.VectorMolecularRecord;

public interface VectorMolecularRecordDAO extends BaseDAO<VectorMolecularRecord, Long> {

    Optional<VectorMolecularRecord> getByIdentificationId(Long identificationId) throws LIMSRuntimeException;
}
