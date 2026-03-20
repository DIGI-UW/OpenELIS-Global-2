package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;

public interface EQAProgramEnrollmentDAO extends BaseDAO<EQAProgramEnrollment, Long> {

    List<EQAProgramEnrollment> findByProgramId(Long programId);

    List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status);

    boolean existsActiveEnrollment(Long programId, Long organizationId);
}
