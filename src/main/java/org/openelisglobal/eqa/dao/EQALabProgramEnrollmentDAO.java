package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentDAO extends BaseDAO<EQALabProgramEnrollment, Long> {

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findByIsActive(Boolean isActive);

    List<String> findDistinctProviders();

    /**
     * Test ids with EQA cover (directly or via a mapped panel, active enrollment
     * only) for the ISO 15189:2022 §7.3.7.3 check, as strings like test.id.
     */
    List<String> findEqaCoveredTestIds();
}
