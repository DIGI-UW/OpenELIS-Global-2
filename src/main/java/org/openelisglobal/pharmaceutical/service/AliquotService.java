package org.openelisglobal.pharmaceutical.service;

import java.util.List;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;

public interface AliquotService {

    Aliquot get(Integer id);

    List<Aliquot> getAll();

    Aliquot save(Aliquot aliquot);

    Aliquot update(Aliquot aliquot);

    void delete(Integer id);

    Aliquot findByBarcode(String barcode);

    List<Aliquot> findByParentSampleId(Integer parentSampleId);

    List<Aliquot> findByStatus(Aliquot.AliquotStatus status);

    List<Aliquot> findByStorageLocation(Integer storageLocationId);

    List<Aliquot> findAvailableByParentSample(Integer parentSampleId);

    List<Aliquot> findExceedingFreezeThawLimit();

    Aliquot createAliquot(Integer parentSampleId, Aliquot aliquot, String userId);

    Aliquot recordFreezeThaw(Integer aliquotId, String userId);

    boolean isFreezeThawLimitExceeded(Integer aliquotId);

    Aliquot updateStatus(Integer aliquotId, Aliquot.AliquotStatus newStatus, String userId);

    String generateAliquotBarcode(Integer parentSampleId, int sequenceNumber);
}
