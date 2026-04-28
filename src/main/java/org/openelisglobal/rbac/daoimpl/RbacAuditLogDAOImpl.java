package org.openelisglobal.rbac.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.rbac.dao.RbacAuditLogDAO;
import org.openelisglobal.rbac.valueholder.RbacAuditLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class RbacAuditLogDAOImpl extends BaseDAOImpl<RbacAuditLog, Long> implements RbacAuditLogDAO {

    public RbacAuditLogDAOImpl() {
        super(RbacAuditLog.class);
    }
}
