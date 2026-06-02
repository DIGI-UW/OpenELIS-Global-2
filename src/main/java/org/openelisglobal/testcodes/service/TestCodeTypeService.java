package org.openelisglobal.testcodes.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.testcodes.valueholder.TestCodeType;

@CrossDomainService(callers = "HL7/FHIR result mapping (all authenticated users), test setup — read methods are public infrastructure; writes go through BaseObjectService guarded at caller")
public interface TestCodeTypeService extends BaseObjectService<TestCodeType, String> {

    TestCodeType getTestCodeTypeById(String id);

    TestCodeType getTestCodeTypeByName(String name);
}
