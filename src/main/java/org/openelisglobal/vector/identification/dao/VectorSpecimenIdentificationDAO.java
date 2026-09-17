package org.openelisglobal.vector.identification.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;

public interface VectorSpecimenIdentificationDAO extends BaseDAO<VectorSpecimenIdentification, Long> {

    Optional<VectorSpecimenIdentification> getBySampleItemId(Long sampleItemId) throws LIMSRuntimeException;

    /**
     * All identifications for a Sample (across its sample_items), ordered by
     * sort_order.
     */
    List<VectorSpecimenIdentification> getBySampleId(Long sampleId) throws LIMSRuntimeException;

    long countBySampleId(Long sampleId) throws LIMSRuntimeException;

    /**
     * Count identified specimens restricted to the given sample_item IDs (one
     * pool's members).
     */
    long countBySampleItemIds(List<Long> sampleItemIds) throws LIMSRuntimeException;
}
