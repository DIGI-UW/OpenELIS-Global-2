package org.openelisglobal.qc.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.SampleItemQcProfile;

/**
 * DAO interface for SampleItemQcProfile entity operations.
 */
public interface SampleItemQcProfileDAO extends BaseDAO<SampleItemQcProfile, String> {

    /**
     * Find the QC profile for a specific sample item (1:1 relationship).
     */
    Optional<SampleItemQcProfile> findBySampleItemId(Integer sampleItemId) throws LIMSRuntimeException;

    /**
     * Find all QC profiles for sample items belonging to a given sample.
     */
    List<SampleItemQcProfile> findBySampleItemIds(List<Integer> sampleItemIds) throws LIMSRuntimeException;
}
