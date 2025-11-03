package org.openelisglobal.storage.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, String> {
    SampleStorageAssignment findBySampleId(String sampleId);
}
