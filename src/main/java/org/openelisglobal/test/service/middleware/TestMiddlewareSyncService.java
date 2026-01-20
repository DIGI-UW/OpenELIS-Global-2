package org.openelisglobal.test.service.middleware;

import org.openelisglobal.test.valueholder.Test;

public interface TestMiddlewareSyncService {

    void syncTestToMiddleware(Test test, boolean isUpdate);
}
