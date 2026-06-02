package org.openelisglobal.vector.identification.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.identification.valueholder.VectorMolecularRecord;

/**
 * V-03 — Service surface for {@link VectorMolecularRecord}. Most callers do not
 * interact with this service directly; the molecular record's lifecycle is
 * orchestrated by {@link VectorSpecimenIdentificationService} as part of the
 * parent identify operation.
 */
public interface VectorMolecularRecordService extends BaseObjectService<VectorMolecularRecord, Long> {

    /** Fetch the molecular record for a given parent identification, if present. */
    Optional<VectorMolecularRecord> getByIdentificationId(Long identificationId);
}
