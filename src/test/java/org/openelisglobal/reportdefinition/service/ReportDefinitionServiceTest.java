/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reportdefinition.service;

import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;

public class ReportDefinitionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    @Before
    @Override
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/reportdefinition.xml");
    }

    @Test
    public void insertReportDefinition_shouldSucceed_whenGivenNewDefinition() {
        ReportDefinition definition = new ReportDefinition();
        definition.setId("REP-TEST-001");
        definition.setName("Test Report");
        definition.setDescription("Test report definition");
        definition.setCategory("TestCategory");
        definition.setDefinitionJson("{\"type\":\"test\"}");
        definition.setCreatedBy("testuser");
        definition.setIsActive(true);

        reportDefinitionService.insert(definition);
    }

    @Test
    public void getReportDefinitionById_shouldReturnEntity_whenDefinitionExists() {
        ReportDefinition definition = new ReportDefinition();
        definition.setId("REP-GET-TEST");
        definition.setName("Get Test Report");
        definition.setDescription("Test for get operation");
        definition.setCategory("TestCategory");
        definition.setDefinitionJson("{\"type\":\"test\"}");
        definition.setCreatedBy("testuser");
        definition.setIsActive(true);

        reportDefinitionService.insert(definition);
        ReportDefinition retrieved = reportDefinitionService.get("REP-GET-TEST");

        Assert.assertNotNull("Retrieved definition should not be null", retrieved);
        Assert.assertEquals("ID should match", "REP-GET-TEST", retrieved.getId());
        Assert.assertEquals("Name should match", "Get Test Report", retrieved.getName());
        Assert.assertEquals("Category should match", "TestCategory", retrieved.getCategory());
    }

    @Test
    public void updateReportDefinition_shouldUpdateValues_whenDefinitionIsModified() {
        ReportDefinition definition = new ReportDefinition();
        definition.setId("REP-UPDATE-TEST");
        definition.setName("Update Test Report");
        definition.setDescription("Original description");
        definition.setCategory("OriginalCategory");
        definition.setDefinitionJson("{\"type\":\"test\"}");
        definition.setCreatedBy("testuser");
        definition.setIsActive(true);

        reportDefinitionService.insert(definition);

        definition.setName("Updated Name");
        definition.setDescription("Updated description");
        definition.setCategory("UpdatedCategory");
        definition.setSysUserId("testuser");

        reportDefinitionService.update(definition);

        ReportDefinition updated = reportDefinitionService.get("REP-UPDATE-TEST");
        Assert.assertEquals("Name should be updated", "Updated Name", updated.getName());
        Assert.assertEquals("Description should be updated", "Updated description", updated.getDescription());
        Assert.assertEquals("Category should be updated", "UpdatedCategory", updated.getCategory());
    }

    @Test
    public void deleteReportDefinition_shouldRemoveEntity_whenDefinitionExists() {
        ReportDefinition definition = new ReportDefinition();
        definition.setId("REP-DELETE-TEST");
        definition.setName("Delete Test Report");
        definition.setDescription("Test for delete operation");
        definition.setCategory("TestCategory");
        definition.setDefinitionJson("{\"type\":\"test\"}");
        definition.setCreatedBy("testuser");
        definition.setIsActive(true);

        reportDefinitionService.insert(definition);

        definition.setSysUserId("testuser");
        reportDefinitionService.delete(definition);

        try {
            reportDefinitionService.get("REP-DELETE-TEST");
            Assert.fail("Should throw ObjectNotFoundException for deleted definition");
        } catch (ObjectNotFoundException e) {
        }
    }
}