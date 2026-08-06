package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

public interface MicroPatientOriginDAO extends BaseDAO<MicroPatientOrigin, String> {
    List<MicroPatientOrigin> getActivePatientOrigins();

    boolean existsActiveCode(String code);
}
