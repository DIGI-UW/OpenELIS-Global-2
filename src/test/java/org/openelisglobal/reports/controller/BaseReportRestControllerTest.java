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
package org.openelisglobal.reports.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Controller integration tests for BaseReportRestController.
 *
 * <p>
 * Tests that the /rest/reports namespace is properly established and
 * accessible.
 */
@RunWith(SpringRunner.class)
public class BaseReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BaseReportRestController baseReportRestController;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testReportsNamespaceExists_Returns200() throws Exception {
        // Test that the /rest/reports namespace is accessible
        this.mockMvc.perform(get("/rest/reports").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
