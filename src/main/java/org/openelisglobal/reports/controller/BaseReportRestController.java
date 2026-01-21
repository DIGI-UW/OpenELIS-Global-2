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

import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base REST Controller for reporting endpoints.
 *
 * <p>
 * Establishes the `/rest/reports` namespace for all reporting-related APIs.
 * Specific reporting endpoints (patient reports, test reports, etc.) are
 * implemented in specialized controllers extending this base class.
 */
@RestController
@RequestMapping("/rest/reports")
public class BaseReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(BaseReportRestController.class);
}
