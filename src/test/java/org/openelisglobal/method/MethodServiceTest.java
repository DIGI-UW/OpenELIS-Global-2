package org.openelisglobal.method;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.springframework.beans.factory.annotation.Autowired;

public class MethodServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MethodService methodService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/method.xml");
    }

    @Test
    public void getData() {
        List<Method> methods = methodService.getAll();
        methods.forEach(sampleItem -> System.out.print(sampleItem.getMethodName() + " "));
    }

}
