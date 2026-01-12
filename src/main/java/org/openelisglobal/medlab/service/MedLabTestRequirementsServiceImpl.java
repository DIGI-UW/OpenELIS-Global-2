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
package org.openelisglobal.medlab.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.medlab.dao.MedLabTestRequirementsDAO;
import org.openelisglobal.medlab.valueholder.MedLabTestRequirements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for MedLabTestRequirements operations.
 *
 * <p>
 * Provides configuration data for order-driven sample collection requirements
 * (FR-006, FR-007).
 */
@Service
public class MedLabTestRequirementsServiceImpl extends AuditableBaseObjectServiceImpl<MedLabTestRequirements, Integer>
        implements MedLabTestRequirementsService {

    @Autowired
    private MedLabTestRequirementsDAO medLabTestRequirementsDAO;

    public MedLabTestRequirementsServiceImpl() {
        super(MedLabTestRequirements.class);
    }

    @Override
    protected MedLabTestRequirementsDAO getBaseObjectDAO() {
        return medLabTestRequirementsDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByTestId(Integer testId) {
        return medLabTestRequirementsDAO.getRequirementsByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public MedLabTestRequirements getRequirementsByTestAndSampleType(Integer testId, Integer typeOfSampleId) {
        return medLabTestRequirementsDAO.getRequirementsByTestAndSampleType(testId, typeOfSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getActiveRequirements() {
        return medLabTestRequirementsDAO.getActiveRequirements();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByTestIds(List<Integer> testIds) {
        return medLabTestRequirementsDAO.getRequirementsByTestIds(testIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByDepartment(Integer departmentId) {
        return medLabTestRequirementsDAO.getRequirementsByDepartment(departmentId);
    }
}
