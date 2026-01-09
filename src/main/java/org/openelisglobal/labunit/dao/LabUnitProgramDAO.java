package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labunit.valueholder.LabUnitProgram;

public interface LabUnitProgramDAO extends BaseDAO<LabUnitProgram, String> {

    List<LabUnitProgram> getProgramsByLabUnitId(String labUnitId);

    List<LabUnitProgram> getLabUnitsByProgramId(String programId);

    LabUnitProgram getByLabUnitAndProgramId(String labUnitId, String programId);

    void deleteByLabUnitId(String labUnitId);

    void deleteByProgramId(String programId);
}