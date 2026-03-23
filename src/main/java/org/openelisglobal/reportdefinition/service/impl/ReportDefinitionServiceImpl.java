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
package org.openelisglobal.reportdefinition.service.impl;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.reportdefinition.dao.ReportDefinitionDAO;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ReportDefinition CRUD operations.
 *
 * <p>
 * Extends AuditableBaseObjectServiceImpl to provide standard ORM operations and
 * audit trail support.
 */
@Service
public class ReportDefinitionServiceImpl extends AuditableBaseObjectServiceImpl<ReportDefinition, String>
        implements ReportDefinitionService {

    @Autowired
    protected ReportDefinitionDAO baseObjectDAO;

    public ReportDefinitionServiceImpl() {
        super(ReportDefinition.class);
    }

    @Override
    protected ReportDefinitionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<ReportDefinition> getActiveDefinitions() {
        return baseObjectDAO.getActiveDefinitions();
    }

    @Override
    public List<ReportDefinition> getDefinitionsByCategory(String category) {
        return baseObjectDAO.getDefinitionsByCategory(category);
    }
}
