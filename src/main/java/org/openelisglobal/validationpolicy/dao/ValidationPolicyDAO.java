package org.openelisglobal.validationpolicy.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;

public interface ValidationPolicyDAO extends BaseDAO<ValidationPolicy, Long> {

    /** The policy without a lab unit. */
    Optional<ValidationPolicy> findLabWideDefault();

    /** The lab unit's override, active or not. */
    Optional<ValidationPolicy> findByTestSectionId(Integer testSectionId);

    /** Every lab unit override, deactivated ones included. */
    List<ValidationPolicy> findLabUnitOverrides();
}
