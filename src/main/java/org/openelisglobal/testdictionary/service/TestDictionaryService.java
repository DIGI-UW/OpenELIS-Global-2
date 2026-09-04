package org.openelisglobal.testdictionary.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testdictionary.valueholder.TestDictionary;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
public interface TestDictionaryService extends BaseObjectService<TestDictionary, String> {
    TestDictionary getTestDictionaryForTestId(String testId);
}
