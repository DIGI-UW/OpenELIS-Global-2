package org.openelisglobal.validationpolicy.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;

public interface ValidationPolicyDAO extends BaseDAO<ValidationPolicy, Long> {

    /** The policy without a lab unit. */
    Optional<ValidationPolicy> findLabWideDefault();
}
