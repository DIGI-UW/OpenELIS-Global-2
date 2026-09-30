package org.openelisglobal.microbiology.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOriginDefault;

public interface MicroPatientOriginDefaultDAO extends BaseDAO<MicroPatientOriginDefault, String> {
    String findPatientOriginIdByOrganizationId(String organizationId);
}
