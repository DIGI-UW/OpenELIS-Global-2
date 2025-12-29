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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.test.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.test.dao.DepartmentSampleTypeDAO;
import org.openelisglobal.test.valueholder.DepartmentSampleType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentSampleTypeServiceImpl extends AuditableBaseObjectServiceImpl<DepartmentSampleType, String>
        implements DepartmentSampleTypeService {

    @Autowired
    private DepartmentSampleTypeDAO departmentSampleTypeDAO;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    public DepartmentSampleTypeServiceImpl() {
        super(DepartmentSampleType.class);
    }

    @Override
    protected DepartmentSampleTypeDAO getBaseObjectDAO() {
        return departmentSampleTypeDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSampleType> getByTestSectionId(String testSectionId) {
        return departmentSampleTypeDAO.getByTestSectionId(testSectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getSampleTypesForDepartment(String testSectionId) {
        List<String> sampleTypeIds = departmentSampleTypeDAO.getSampleTypeIdsForTestSection(testSectionId);
        List<TypeOfSample> sampleTypes = new ArrayList<>();
        for (String id : sampleTypeIds) {
            TypeOfSample sampleType = typeOfSampleService.get(id);
            if (sampleType != null) {
                sampleTypes.add(sampleType);
            }
        }
        return sampleTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getActiveSampleTypesForDepartment(String testSectionId) {
        List<DepartmentSampleType> associations = departmentSampleTypeDAO.getActiveByTestSectionId(testSectionId);
        List<TypeOfSample> sampleTypes = new ArrayList<>();
        for (DepartmentSampleType assoc : associations) {
            TypeOfSample sampleType = typeOfSampleService.get(assoc.getSampleTypeId());
            if (sampleType != null && sampleType.isActive()) {
                sampleTypes.add(sampleType);
            }
        }
        return sampleTypes;
    }

    @Override
    @Transactional
    public void linkDepartmentAndSampleType(String testSectionId, String sampleTypeId) {
        linkDepartmentAndSampleType(testSectionId, sampleTypeId, 0);
    }

    @Override
    @Transactional
    public void linkDepartmentAndSampleType(String testSectionId, String sampleTypeId, Integer sortOrder) {
        // Check if link already exists
        DepartmentSampleType existing = departmentSampleTypeDAO.getByTestSectionAndSampleType(testSectionId,
                sampleTypeId);
        if (existing != null) {
            // Update sort order if link exists
            existing.setSortOrder(sortOrder);
            update(existing);
            return;
        }

        DepartmentSampleType link = new DepartmentSampleType();
        link.setTestSectionId(testSectionId);
        link.setSampleTypeId(sampleTypeId);
        link.setIsActive(true);
        link.setSortOrder(sortOrder);
        insert(link);
    }

    @Override
    @Transactional
    public void unlinkDepartmentAndSampleType(String testSectionId, String sampleTypeId) {
        DepartmentSampleType existing = departmentSampleTypeDAO.getByTestSectionAndSampleType(testSectionId,
                sampleTypeId);
        if (existing != null) {
            delete(existing);
        }
    }

    @Override
    @Transactional
    public void deleteAllLinksForDepartment(String testSectionId) {
        departmentSampleTypeDAO.deleteAllForTestSection(testSectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeIdsForDepartment(String testSectionId) {
        return departmentSampleTypeDAO.getSampleTypeIdsForTestSection(testSectionId);
    }

    @Override
    @Transactional
    public void updateSampleTypesForDepartment(String testSectionId, List<String> sampleTypeIds) {
        // Delete existing links
        deleteAllLinksForDepartment(testSectionId);

        // Create new links with sort order
        int sortOrder = 0;
        for (String sampleTypeId : sampleTypeIds) {
            linkDepartmentAndSampleType(testSectionId, sampleTypeId, sortOrder++);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSampleType> getBySampleTypeId(String sampleTypeId) {
        return departmentSampleTypeDAO.getBySampleTypeId(sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTestSectionIdsForSampleType(String sampleTypeId) {
        return departmentSampleTypeDAO.getTestSectionIdsForSampleType(sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getSampleTypesForDepartments(List<String> testSectionIds) {
        if (testSectionIds == null || testSectionIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<TypeOfSample> result = new HashSet<>();
        for (String testSectionId : testSectionIds) {
            result.addAll(getSampleTypesForDepartment(testSectionId));
        }
        return new ArrayList<>(result);
    }
}
