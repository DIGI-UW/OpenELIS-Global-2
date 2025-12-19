package org.openelisglobal.storage.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, Integer> {
    SampleStorageAssignment findBySampleItemId(String sampleItemId);

    SampleStorageAssignment findByStorageBox(StorageBox box);

    boolean isBoxOccupied(StorageBox box);

    SampleStorageAssignment findByBoxAndCoordinate(Integer boxId, String coordinate);

    List<String> getOccupiedCoordinatesByBoxId(Integer boxId);

    Map<String, Map<String, String>> getOccupiedCoordinatesWithSampleInfo(Integer boxId);

    int countByLocationTypeAndId(String locationType, Integer locationId);

    /**
     * Find all sample storage assignments with pagination support (OGC-150).
     *
     * @param pageable Pagination parameters (page number, page size, sorting)
     * @return Page of SampleStorageAssignment entities
     */
    Page<SampleStorageAssignment> findAll(Pageable pageable);
}
