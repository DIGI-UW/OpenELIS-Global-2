package org.openelisglobal.testactivation.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testactivation.valueholder.TestActivationAcknowledgment;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface TestActivationAcknowledgmentService extends BaseObjectService<TestActivationAcknowledgment, String> {
}
