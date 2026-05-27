package org.openelisglobal.testcodes.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcodes.valueholder.TestCodeType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestCodeTypeService extends BaseObjectService<TestCodeType, String> {

    // Used during HL7/FHIR mapping — not admin-only
    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    TestCodeType getTestCodeTypeById(String id);

    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    TestCodeType getTestCodeTypeByName(String name);
}
