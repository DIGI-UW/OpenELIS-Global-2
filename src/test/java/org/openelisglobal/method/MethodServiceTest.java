package org.openelisglobal.method;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
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

    // ----------------------------------------------------------------
    // NEW TESTS — covering previously untested methods
    // ----------------------------------------------------------------

    @Test
    public void deleteShouldSetMethodToInactive() throws Exception {
        // "therapy" (id=1) is active in the test dataset
        Method method = mService.get("1");
        Assert.assertEquals("Y", method.getIsActive());

        method.setSysUserId("1");
        mService.delete(method);

        Method deletedMethod = mService.get("1");
        Assert.assertEquals("N", deletedMethod.getIsActive());
    }

    @Test
    public void deleteShouldReduceActiveMethodCount() throws Exception {
        Method method = mService.get("1");
        method.setSysUserId("1");
        mService.delete(method);

        List<Method> activeMethods = mService.getAllActiveMethods();
        Assert.assertEquals(2, activeMethods.size());
    }

    @Test
    public void deleteShouldIncreaseInactiveMethodCount() throws Exception {
        Method method = mService.get("1");
        method.setSysUserId("1");
        mService.delete(method);

        List<Method> inactiveMethods = mService.getAllInActiveMethods();
        Assert.assertEquals(2, inactiveMethods.size());
    }

    @Test
    public void saveShouldPersistNewMethod() throws Exception {
        Method method = new Method();
        method.setMethodName("Ultrasound");
        method.setDescription("sound wave imaging");
        method.setIsActive("Y");
        method.setSysUserId("1");

        Method savedMethod = mService.save(method);

        Assert.assertNotNull(savedMethod.getId());
        Assert.assertEquals("Ultrasound", mService.get(savedMethod.getId()).getMethodName());
    }

    @Test
    public void saveShouldUpdateExistingMethod() throws Exception {
        Method method = mService.get("1");
        method.setDescription("updated description");
        method.setSysUserId("1");

        mService.save(method);

        Method updatedMethod = mService.get("1");
        Assert.assertEquals("updated description", updatedMethod.getDescription());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void saveShouldThrowExceptionForDuplicateMethod() throws Exception {
        // "therapy" already exists in the dataset — saving another with same name
        // should throw
        Method duplicate = new Method();
        duplicate.setMethodName("therapy");
        duplicate.setDescription("duplicate entry");
        duplicate.setIsActive("Y");
        duplicate.setSysUserId("1");

        mService.save(duplicate);
    }

    @Test
    public void updateShouldModifyExistingMethod() throws Exception {
        Method method = mService.get("1");
        method.setDescription("updated via update()");
        method.setSysUserId("1");

        mService.update(method);

        Method updatedMethod = mService.get("1");
        Assert.assertEquals("updated via update()", updatedMethod.getDescription());
    }

    @Test
    public void updateShouldToggleActiveStatus() throws Exception {
        // "imagining" (id=2) is inactive — update it to active
        Method method = mService.get("2");
        method.setIsActive("Y");
        method.setSysUserId("1");

        mService.update(method);

        Method updatedMethod = mService.get("2");
        Assert.assertEquals("Y", updatedMethod.getIsActive());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void updateShouldThrowExceptionForDuplicateName() throws Exception {
        // Try renaming "hologram" (id=3) to "therapy" which already exists
        Method method = mService.get("3");
        method.setMethodName("therapy");
        method.setSysUserId("1");

        mService.update(method);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void insertShouldThrowExceptionForDuplicateName() throws Exception {
        // "surgery" already exists in the dataset
        Method duplicate = new Method();
        duplicate.setMethodName("surgery");
        duplicate.setDescription("duplicate surgery entry");
        duplicate.setIsActive("Y");
        duplicate.setSysUserId("1");

        mService.insert(duplicate);
    }

    @Test
public void methodNamesChangedShouldPopulateIdToNameMap() throws Exception {
    mService.refreshNames(); 

    Map<String, String> map = mService.getMethodUnitIdToNameMap();

    Assert.assertNotNull(map);
    Assert.assertFalse(map.isEmpty());
    Assert.assertEquals(4, map.size());
}


    @Test
public void methodNamesChangedShouldMapCorrectLocalization() throws Exception {
    mService.refreshNames(); 

    Map<String, String> map = mService.getMethodUnitIdToNameMap();

    Assert.assertEquals("therapy Method", map.get("1"));
    Assert.assertEquals("imagining Method", map.get("2"));
    Assert.assertEquals("hologram Method", map.get("3"));
    Assert.assertEquals("surgery Method", map.get("4"));
}

    @Test
    public void getMethodUnitIdToNameMapShouldReturnNullBeforeRefresh() throws Exception {
        // The map is only populated after refreshNames() or methodNamesChanged()
        // A fresh context without calling refresh should return null
        Assert.assertNull(mService.getMethodUnitIdToNameMap());
    }

    @Test
    public void getMethodsShouldReturnEmptyListWhenNoMatch() throws Exception {
        List<Method> filteredMethods = mService.getMethods("zzznomatch");
        Assert.assertNotNull(filteredMethods);
        Assert.assertTrue(filteredMethods.isEmpty());
    }

    @Test
    public void getMethodsShouldBeCaseInsensitive() throws Exception {
        List<Method> filteredMethods = mService.getMethods("T");
        Assert.assertFalse(filteredMethods.isEmpty());
        Assert.assertEquals("therapy", filteredMethods.get(0).getMethodName());
    }
}
