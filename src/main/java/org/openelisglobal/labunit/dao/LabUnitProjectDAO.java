package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labunit.valueholder.LabUnitProject;

/**
 * DAO interface for Lab Unit Project operations.
 */
public interface LabUnitProjectDAO extends BaseDAO<LabUnitProject, String> {

    List<LabUnitProject> getProjectsByLabUnitId(String labUnitId);

    List<LabUnitProject> getLabUnitsByProjectId(String projectId);

    LabUnitProject getByLabUnitAndProjectId(String labUnitId, String projectId);

    void deleteByLabUnitId(String labUnitId);

    void deleteByProjectId(String projectId);
}