package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;

public interface EQAProgramDAO extends BaseDAO<EQAProgram, Long> {

    List<EQAProgram> findByIsActive(Boolean isActive);

    /**
     * The scheme type as the database currently holds it, read without consulting
     * the persistence context. A caller that has already changed the type on a
     * managed scheme must still be able to see what it is changing away from.
     */
    EQASchemeType findStoredSchemeType(Long id);
}
