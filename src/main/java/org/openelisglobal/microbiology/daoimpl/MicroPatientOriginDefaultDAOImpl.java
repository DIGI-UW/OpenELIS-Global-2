package org.openelisglobal.microbiology.daoimpl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDefaultDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOriginDefault;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroPatientOriginDefaultDAOImpl extends BaseDAOImpl<MicroPatientOriginDefault, String>
        implements MicroPatientOriginDefaultDAO {

    public MicroPatientOriginDefaultDAOImpl() {
        super(MicroPatientOriginDefault.class);
    }

    @Override
    @Transactional(readOnly = true)
    public String findPatientOriginIdByOrganizationId(String organizationId) {
        Query<String> query = entityManager.unwrap(Session.class).createQuery(
                "select d.patientOriginId from MicroPatientOriginDefault d where d.organizationId = :organizationId",
                String.class);
        query.setParameter("organizationId", organizationId);
        return query.uniqueResultOptional().orElse(null);
    }
}
