package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;

/**
 * DAO for SampleStorageMovement - Audit log (insert-only)
 */
public interface SampleStorageMovementDAO extends BaseDAO<SampleStorageMovement, Integer> {
    List<SampleStorageMovement> findBySampleItemId(String sampleItemId);
}
