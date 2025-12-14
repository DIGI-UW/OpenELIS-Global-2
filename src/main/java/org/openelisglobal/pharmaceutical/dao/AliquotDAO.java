package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;

public interface AliquotDAO extends BaseDAO<Aliquot, Integer> {

    List<Aliquot> findByParentSampleId(Integer parentSampleId);

    Aliquot findByBarcode(String barcode);

    List<Aliquot> findByStatus(Aliquot.AliquotStatus status);

    List<Aliquot> findByStorageLocation(Integer storageLocationId, String storageLocationType);

    List<Aliquot> findAvailableByParentSample(Integer parentSampleId);

    List<Aliquot> findExceedingFreezeThawLimit();
}
