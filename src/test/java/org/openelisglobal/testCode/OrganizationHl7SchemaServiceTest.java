package org.openelisglobal.testCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testcodes.service.OrganizationHL7SchemaService;
import org.openelisglobal.testcodes.valueholder.OrganizationHL7Schema;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationHl7SchemaServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrganizationHL7SchemaService organizationHl7SchemaService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/organization-hl7-schema.xml");

    }

    @Test
    public void getAll_shouldReturnAllOrganizationHl7Schemas() {
        List<OrganizationHL7Schema> schemas = organizationHl7SchemaService.getAll();
        assertNotNull(schemas);
        assertEquals(3, schemas.size());
        assertEquals("3", schemas.get(0).getCompoundId().getOrganizationId());
        assertEquals("4", schemas.get(1).getCompoundId().getOrganizationId());
        assertEquals("5", schemas.get(2).getCompoundId().getOrganizationId());

    }

}
