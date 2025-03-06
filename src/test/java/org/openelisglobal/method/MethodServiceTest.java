package org.openelisglobal.method;

import static org.junit.Assert.assertEquals;

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
    public void getDataFromDataBase() {
        List<Method> methods = methodService.getAll();
        methods.forEach(sampleItem -> System.out.print(sampleItem.getMethodName() + " "));
    }

    @Test
    public void getMethods_shouldReturnMethodsGivenCriteria() {
        List<Method> methods = methodService.getMethods("PCR");
        methods.forEach(sampleItem -> assertEquals("PCR", sampleItem.getMethodName()));
    }

    @Test
    public void deleteMethod_shouldSetIsActiveToNo() {
        Method method = methodService.get("1");
        methodService.delete(method);
        assertEquals("N", method.getIsActive());

    }

    @Test
    public void getAllActiveMethods_shouldReturnActiveMethods() {
        List<Method> methods = methodService.getAllActiveMethods();
        methods.forEach(sampleItem -> assertEquals("Y", sampleItem.getIsActive()));
        assertEquals(2, methods.size());

    }

    @Test
    public void getAllInActiveMethods_shouldReturnInActiveMethods() {
        List<Method> methods = methodService.getAllInActiveMethods();
        methods.forEach(sampleItem -> assertEquals("N", sampleItem.getIsActive()));
        assertEquals(2, methods.size());
    }

}
