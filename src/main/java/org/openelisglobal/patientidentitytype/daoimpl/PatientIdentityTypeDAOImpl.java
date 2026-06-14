/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.patientidentitytype.daoimpl;

import java.util.List;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patientidentitytype.dao.PatientIdentityTypeDAO;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PatientIdentityTypeDAOImpl extends BaseDAOImpl<PatientIdentityType, String>
        implements PatientIdentityTypeDAO {

    public PatientIdentityTypeDAOImpl() {
        super(PatientIdentityType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientIdentityType> getAllPatientIdenityTypes() throws LIMSRuntimeException {
        List<PatientIdentityType> list = null;
        try {
            String sql = "from PatientIdentityType";
            Query<PatientIdentityType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    PatientIdentityType.class);

            list = query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllPatientIdenityTypes");
        }

        return list;
    }

    @Override
    public boolean duplicatePatientIdentityTypeExists(PatientIdentityType patientIdentityType)
            throws LIMSRuntimeException {
        try {
            // Exclude the row being saved (t.id != :id) so updating a type — or
            // re-saving it after Hibernate has flushed the change — is not flagged as
            // a duplicate of itself. New records have no id yet, so use a sentinel
            // that matches no existing row. Mirrors duplicateGenderExists() et al.
            String sql = "from PatientIdentityType t where upper(t.identityType) = :identityType and t.id != :id";
            Query<PatientIdentityType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    PatientIdentityType.class);

            query.setParameter("identityType", patientIdentityType.getIdentityType().toUpperCase());
            String id = patientIdentityType.getId();
            query.setParameter("id", (id == null || id.isEmpty()) ? "0" : id);

            // Check against COMMITTED state, not the caller's pending change. The
            // caller has already mutated the managed entity (e.g. set a duplicate
            // identityType); a default auto-flush would push that UPDATE first and
            // trip the identity_type_uk unique constraint, surfacing a raw
            // ConstraintViolationException instead of letting us return true and throw
            // a clean LIMSDuplicateRecordException.
            query.setHibernateFlushMode(FlushMode.COMMIT);

            List<PatientIdentityType> list = query.list();
            return list.size() > 0;
        } catch (HibernateException e) {
            handleException(e, "duplicatePatientIdentityTypeExists");
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientIdentityType getNamedIdentityType(String name) throws LIMSRuntimeException {
        String sql = "from PatientIdentityType t where t.identityType = :identityType";

        try {
            Query<PatientIdentityType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    PatientIdentityType.class);
            query.setParameter("identityType", name);
            PatientIdentityType pit = query.uniqueResult();
            return pit;
        } catch (HibernateException e) {
            handleException(e, "getNamedIdentityType");
        }

        return null;
    }
}
