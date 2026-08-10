package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;

public interface EQALabProgramEnrollmentDAO extends BaseDAO<EQALabProgramEnrollment, Long> {

    List<EQALabProgramEnrollment> findAll();

    List<EQALabProgramEnrollment> findByIsActive(Boolean isActive);

    List<String> findDistinctProviders();

    /**
     * Ids of every test the lab currently has EQA cover for — mapped directly or
     * through a mapped panel, under an enrollment that is still active.
     *
     * <p>
     * OGC-686 (QA-D.5): the accreditation page joins this against accredited scope
     * to answer ISO 15189 §7.7 ("is every accredited test in an EQA scheme?"). Ids
     * come back as strings because that is how {@code test.id} is carried
     * everywhere outside this module.
     */
    List<String> findEqaCoveredTestIds();
}
