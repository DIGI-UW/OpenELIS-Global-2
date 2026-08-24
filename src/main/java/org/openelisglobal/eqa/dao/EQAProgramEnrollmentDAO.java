package org.openelisglobal.eqa.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;

public interface EQAProgramEnrollmentDAO extends BaseDAO<EQAProgramEnrollment, Long> {

    List<EQAProgramEnrollment> findByProgramId(Long programId);

    List<EQAProgramEnrollment> findByProgramIdAndStatus(Long programId, String status);

    boolean existsActiveEnrollment(Long programId, Long organizationId);

    /**
     * The schemes this lab provides, with their active participant count, in one
     * grouped query (FR-V2.5-01). "Provides" means at least one other laboratory is
     * actively enrolled — OpenELIS is single-tenant, so that enrollment is what
     * distinguishes a scheme this lab runs from one it merely takes part in.
     *
     * <p>
     * Each row is
     * {@code [schemeId, name, provider, schemeType, activeParticipants]} — a
     * projection rather than entities, because nothing here needs a managed scheme.
     * Replaces T-25's walk over every cycle asking each scheme for its count.
     */
    List<Object[]> findProviderSchemeRows();
}
