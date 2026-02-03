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

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;

/**
 * Service interface for ReportDefinition CRUD operations.
 *
 * <p>
 * Provides business logic for managing user-created ad-hoc report definitions.
 */
public interface ReportDefinitionService extends BaseObjectService<ReportDefinition, String> {

    /**
     * Get all active report definitions.
     *
     * @return list of active report definitions
     */
    List<ReportDefinition> getActiveDefinitions();

    /**
     * Get report definitions by category.
     *
     * @param category report category
     * @return list of report definitions matching the category
     */
    List<ReportDefinition> getDefinitionsByCategory(String category);
}
