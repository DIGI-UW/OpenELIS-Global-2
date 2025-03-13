package org.openelisglobal.method;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.springframework.beans.factory.annotation.Autowired;

public class MethodServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    MethodService mService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/method.xml");
    }

    @Test
    public void createMethodShouldReturnNewMethod() throws Exception {

        Date date = Date.valueOf("1992-12-12");

        Method method = new Method();
        method.setMethodName("Questionnaire");
        method.setDescription("asking questions");
        method.setActiveBeginDate(date);
        method.setIsActive("Y");

        String methodId = mService.insert(method);
        Method createMethod = mService.get(methodId);

        Assert.assertEquals("Questionnaire", createMethod.getMethodName());
        Assert.assertEquals("asking questions", createMethod.getDescription());
    }

    @Test
    public void getAllActiveMethodsReturnAllActiveMethods() throws Exception {
        List<Method> methods = mService.getAllActiveMethods();

        Assert.assertEquals(3, methods.size());
        Assert.assertEquals("therapy", methods.get(0).getMethodName());
        Assert.assertEquals("surgery", methods.get(2).getMethodName());
    }

    @Test
    public void getAllInActiveMethodsReturnAllInActiveMethods() throws Exception {

        List<Method> methods = mService.getAllInActiveMethods();

        Assert.assertEquals(1, methods.size());
        Assert.assertEquals("imagining", methods.get(0).getMethodName());
    }

    @Test
    public void refreshNamesRefreshNames() throws Exception {
        mService.refreshNames();

        Map<String, String> methodMap = mService.getMethodUnitIdToNameMap();

        Assert.assertEquals("therapy Method", methodMap.get("1"));
    }

    @Test
    public void getMethodsReturnAllFilteredMethods() throws Exception {
        List<Method> filteredMethods = mService.getMethods("t");
        Assert.assertEquals(1, filteredMethods.size());
        Assert.assertEquals("therapy", filteredMethods.get(0).getMethodName());
    }
}
