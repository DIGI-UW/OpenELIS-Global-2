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
package org.openelisglobal.reportdefinition.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * REST Controller integration tests for Report Definition endpoints.
 */
@RunWith(SpringRunner.class)
public class ReportDefinitionRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReportDefinitionService reportDefinitionService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/reportdefinition.xml");
    }

    @Test
    public void getDefinitions_shouldReturnAllReportDefinitions_whenCalled() throws Exception {
        this.mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/rest/reports/definitions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3));
    }

    @Test
    public void getDefinition_shouldReturnReportDefinition_whenGivenValidId() throws Exception {
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/rest/reports/definitions/REP-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("REP-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Test Report 1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.category").value("Patient"));
    }

    @Test
    public void getDefinition_shouldReturnNotFound_whenGivenInvalidId() throws Exception {
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/rest/reports/definitions/INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void getActiveDefinitions_shouldReturnOnlyActiveReports_whenCalled() throws Exception {
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/rest/reports/definitions/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2));
    }
}
