package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;

public interface EQAProgramTestDAO extends BaseDAO<EQAProgramTest, Long> {

    List<EQAProgramTest> findByProgramId(Long programId);

    List<EQAProgramTest> findActiveByProgramId(Long programId);
}
