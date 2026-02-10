package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;

public interface EQAProgramDAO extends BaseDAO<EQAProgram, Long> {

    List<EQAProgram> findByIsActive(Boolean isActive);
}
