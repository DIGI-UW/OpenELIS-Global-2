package org.openelisglobal.test.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.AssignableTest;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface AssignableTestService extends BaseObjectService<AssignableTest, String> {
}
