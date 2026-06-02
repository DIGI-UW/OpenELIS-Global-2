package org.openelisglobal.testcodes.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcodes.valueholder.OrganizationHL7Schema;
import org.openelisglobal.testcodes.valueholder.OrganizationSchemaPK;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface OrganizationHL7SchemaService extends BaseObjectService<OrganizationHL7Schema, OrganizationSchemaPK> {
}
