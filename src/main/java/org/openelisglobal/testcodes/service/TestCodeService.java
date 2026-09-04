package org.openelisglobal.testcodes.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcodes.valueholder.TestCode;
import org.openelisglobal.testcodes.valueholder.TestSchemaPK;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface TestCodeService extends BaseObjectService<TestCode, TestSchemaPK> {
}
