package org.openelisglobal.requester;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.springframework.beans.factory.annotation.Autowired;

public class RequesterTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    RequesterTypeService requesterTypeService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/requester.xml");
    }

    // @Test
    public void verifyRequesterTypeData() {
        List<RequesterType> requesterTypes = requesterTypeService.getAll();
        System.out.println("Requester Types in DB: " + requesterTypes.size());
        requesterTypes.forEach(type -> System.out.println(type.getId() + " - " + type.getRequesterType()));

        Assert.assertFalse("❌ requester_type table should not be empty!", requesterTypes.isEmpty());
    }

    @Test
    public void getRequesterTypeByName_shouldReturnCorrectRequesterType() {
        String typeName = "LABORATORY";
        RequesterType requesterType = requesterTypeService.getRequesterTypeByName(typeName);
        Assert.assertNotNull("Requester type should not be null", requesterType);
        Assert.assertEquals("Expected requester type name to be LABORATORY", "LABORATORY",
                requesterType.getRequesterType());
    }

    @Test
    public void getAll_shouldReturnAllRequesterTypes() {
        List<RequesterType> requesterTypes = requesterTypeService.getAll();
        Assert.assertNotNull("Requester types list should not be null", requesterTypes);
        Assert.assertTrue("Requester types list should not be empty", !requesterTypes.isEmpty());
    }
}
