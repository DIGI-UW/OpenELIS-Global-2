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
package org.openelisglobal.organization.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.organization.dao.OrganizationSampleTypeDAO;
import org.openelisglobal.organization.valueholder.OrganizationSampleType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationSampleTypeServiceImpl extends AuditableBaseObjectServiceImpl<OrganizationSampleType, String>
        implements OrganizationSampleTypeService {

    @Autowired
    private OrganizationSampleTypeDAO organizationSampleTypeDAO;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    public OrganizationSampleTypeServiceImpl() {
        super(OrganizationSampleType.class);
    }

    @Override
    protected OrganizationSampleTypeDAO getBaseObjectDAO() {
        return organizationSampleTypeDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSampleType> getByOrganizationId(String organizationId) {
        return organizationSampleTypeDAO.getByOrganizationId(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getSampleTypesForOrganization(String organizationId) {
        List<String> sampleTypeIds = organizationSampleTypeDAO.getSampleTypeIdsForOrganization(organizationId);
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
    public List<TypeOfSample> getActiveSampleTypesForOrganization(String organizationId) {
        List<OrganizationSampleType> associations = organizationSampleTypeDAO.getActiveByOrganizationId(organizationId);
        List<TypeOfSample> sampleTypes = new ArrayList<>();
        for (OrganizationSampleType assoc : associations) {
            TypeOfSample sampleType = typeOfSampleService.get(assoc.getSampleTypeId());
            if (sampleType != null && sampleType.isActive()) {
                sampleTypes.add(sampleType);
            }
        }
        return sampleTypes;
    }

    @Override
    @Transactional
    public void linkOrganizationAndSampleType(String organizationId, String sampleTypeId) {
        linkOrganizationAndSampleType(organizationId, sampleTypeId, 0);
    }

    @Override
    @Transactional
    public void linkOrganizationAndSampleType(String organizationId, String sampleTypeId, Integer sortOrder) {
        // Check if link already exists
        OrganizationSampleType existing = organizationSampleTypeDAO.getByOrganizationAndSampleType(organizationId,
                sampleTypeId);
        if (existing != null) {
            // Update sort order if link exists
            existing.setSortOrder(sortOrder);
            update(existing);
            return;
        }

        OrganizationSampleType link = new OrganizationSampleType();
        link.setOrganizationId(organizationId);
        link.setSampleTypeId(sampleTypeId);
        link.setIsActive(true);
        link.setSortOrder(sortOrder);
        insert(link);
    }

    @Override
    @Transactional
    public void unlinkOrganizationAndSampleType(String organizationId, String sampleTypeId) {
        OrganizationSampleType existing = organizationSampleTypeDAO.getByOrganizationAndSampleType(organizationId,
                sampleTypeId);
        if (existing != null) {
            delete(existing);
        }
    }

    @Override
    @Transactional
    public void deleteAllLinksForOrganization(String organizationId) {
        organizationSampleTypeDAO.deleteAllForOrganization(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeIdsForOrganization(String organizationId) {
        return organizationSampleTypeDAO.getSampleTypeIdsForOrganization(organizationId);
    }

    @Override
    @Transactional
    public void updateSampleTypesForOrganization(String organizationId, List<String> sampleTypeIds) {
        // Delete existing links
        deleteAllLinksForOrganization(organizationId);

        // Create new links with sort order
        int sortOrder = 0;
        for (String sampleTypeId : sampleTypeIds) {
            linkOrganizationAndSampleType(organizationId, sampleTypeId, sortOrder++);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSampleType> getBySampleTypeId(String sampleTypeId) {
        return organizationSampleTypeDAO.getBySampleTypeId(sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getOrganizationIdsForSampleType(String sampleTypeId) {
        return organizationSampleTypeDAO.getOrganizationIdsForSampleType(sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getSampleTypesForOrganizations(List<String> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<TypeOfSample> result = new HashSet<>();
        for (String orgId : organizationIds) {
            result.addAll(getSampleTypesForOrganization(orgId));
        }
        return new ArrayList<>(result);
    }
}
