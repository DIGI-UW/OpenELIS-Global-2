package org.openelisglobal.requester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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

    @Test
    public void verifyRequesterTypeData() {
        List<RequesterType> requesterTypes = requesterTypeService.getAll();

        assertNotNull("Requester type list should not be null", requesterTypes);
        assertFalse("Requester type list should not be empty", requesterTypes.isEmpty());

        requesterTypes.forEach(type -> {
            assertNotNull("RequesterType ID should not be null", type.getId());
            assertNotNull("RequesterType name should not be null", type.getRequesterType());
        });
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
