package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labunit.valueholder.LabUnit;
import org.openelisglobal.labunit.valueholder.LabUnitAssignment;

/**
 * DAO interface for Lab Unit operations. Simplified to avoid dependency issues.
 */
public interface LabUnitDAO extends BaseDAO<LabUnit, String> {

    List<LabUnit> getAllLabUnits();

    List<LabUnit> getActiveLabUnits();

    List<LabUnit> getInactiveLabUnits();

    LabUnit getLabUnitById(String id);

    LabUnit getLabUnitByName(String name);

    // Assignment operations
    List<LabUnitAssignment> getAssignmentsForLabUnit(String labUnitId);

    boolean hasAssignments(String labUnitId);

    // Count operations
    int getTotalLabUnitCount();

    int getActiveLabUnitCount();

    int getInactiveLabUnitCount();

    // Ordering operations
    void updateSortOrderForMultiple(List<LabUnit> labUnits);

    // Search and filtering
    List<LabUnit> getLabUnitsByNameFilter(String filter);

    // Validation operations
    boolean isDuplicateName(String name, String excludeId);

    boolean isDuplicateCode(String code, String excludeId);
}