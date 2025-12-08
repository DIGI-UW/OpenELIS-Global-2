package org.openelisglobal.storage.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageBox;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, Integer> {
    SampleStorageAssignment findBySampleItemId(String sampleItemId);

    SampleStorageAssignment findByStorageBox(StorageBox box);

    boolean isBoxOccupied(StorageBox box);

    SampleStorageAssignment findByBoxAndCoordinate(Integer boxId, String coordinate);

    List<String> getOccupiedCoordinatesByBoxId(Integer boxId);

    Map<String, Map<String, String>> getOccupiedCoordinatesWithSampleInfo(Integer boxId);

    int countByLocationTypeAndId(String locationType, Integer locationId);
}
