package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;

public interface MicroPatientOriginDAO extends BaseDAO<MicroPatientOrigin, String> {
    List<MicroPatientOrigin> getActivePatientOrigins();

    List<MicroPatientOrigin> getByCodes(List<String> codes);

    boolean existsActiveCode(String code);

    List<MicroPatientOrigin> search(String q, String status, String sort, int offset, int limit);

    long countSearch(String q, String status);
}
