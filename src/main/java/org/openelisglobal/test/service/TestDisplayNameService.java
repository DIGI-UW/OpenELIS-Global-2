package org.openelisglobal.test.service;

import java.util.Map;
import org.openelisglobal.test.valueholder.Test;

/**
 * Context-owned test display-name state used by legacy static naming entry
 * points.
 */
public interface TestDisplayNameService {

    enum NameMap {
        TEST_NAME, TEST_AUGMENTED_NAME, TEST_REPORTING_NAME
    }

    Map<String, String> getNameMap(NameMap nameMap);

    String localizeNameWithType(Test test);

    String localizeNameWithType(Test test, String sampleTypeName);

    String localizeNameWithAllTypes(Test test);
}
