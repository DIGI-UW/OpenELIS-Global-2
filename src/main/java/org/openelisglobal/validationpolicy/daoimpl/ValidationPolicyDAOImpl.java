package org.openelisglobal.validationpolicy.daoimpl;

import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.validationpolicy.dao.ValidationPolicyDAO;
import org.openelisglobal.validationpolicy.valueholder.ValidationPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ValidationPolicyDAOImpl extends BaseDAOImpl<ValidationPolicy, Long> implements ValidationPolicyDAO {

    public ValidationPolicyDAOImpl() {
        super(ValidationPolicy.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ValidationPolicy> findLabWideDefault() {
        return entityManager
                .createQuery("FROM ValidationPolicy p WHERE p.testSectionId IS NULL", ValidationPolicy.class)
                .getResultList().stream().findFirst();
    }
}
