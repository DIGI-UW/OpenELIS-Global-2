package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentDAO extends BaseDAO<EQALabProgramEnrollment, Long> {

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findByIsActive(Boolean isActive);

    List<String> findDistinctProviders();
}
