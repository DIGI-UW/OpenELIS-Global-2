package org.openelisglobal.testcodes.service;

import java.util.ArrayList;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testcodes.dao.OrganizationHL7SchemaDAO;
import org.openelisglobal.testcodes.valueholder.OrganizationHL7Schema;
import org.openelisglobal.testcodes.valueholder.OrganizationSchemaPK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationHL7SchemaServiceImpl
        extends AuditableBaseObjectServiceImpl<OrganizationHL7Schema, OrganizationSchemaPK>
        implements OrganizationHL7SchemaService {
    @Autowired
    protected OrganizationHL7SchemaDAO baseObjectDAO;

    OrganizationHL7SchemaServiceImpl() {
        super(OrganizationHL7Schema.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected OrganizationHL7SchemaDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
