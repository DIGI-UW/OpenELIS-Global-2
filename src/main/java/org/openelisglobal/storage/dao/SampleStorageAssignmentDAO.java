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

    SampleStorageAssignment findByInventoryLotId(Long inventoryLotId);

    List<SampleStorageAssignment> findByInventoryLotIds(List<Long> inventoryLotIds);

    /** Assignments whose occupant is of the given type, sample or inventory lot. */
    List<SampleStorageAssignment> findByOccupantType(String occupantType);

    SampleStorageAssignment findByStorageBox(StorageBox box);

    boolean isBoxOccupied(StorageBox box);

    SampleStorageAssignment findByBoxAndCoordinate(Integer boxId, String coordinate);

    List<String> getOccupiedCoordinatesByBoxId(Integer boxId);

    /**
     * Occupied box coordinates to occupant info: occupantType, sampleItemId or
     * inventoryLotId, and externalId (sample item external id or lot number).
     */
    Map<String, Map<String, String>> getOccupiedCoordinatesWithOccupantInfo(Integer boxId);

    int countByLocationTypeAndId(String locationType, Integer locationId);

    /**
     * Find all sample storage assignments with pagination support (OGC-150).
     *
     * @param pageable Pagination parameters (page number, page size, sorting)
     * @return Page of SampleStorageAssignment entities
     */
    Page<SampleStorageAssignment> findAll(Pageable pageable);
}
