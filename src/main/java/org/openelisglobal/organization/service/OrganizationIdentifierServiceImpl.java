package org.openelisglobal.organization.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.organization.dao.OrganizationIdentifierDAO;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationIdentifierServiceImpl extends BaseObjectServiceImpl<OrganizationIdentifier, Integer>
        implements OrganizationIdentifierService {

    @Autowired
    protected OrganizationIdentifierDAO baseObjectDAO;

    public OrganizationIdentifierServiceImpl() {
        super(OrganizationIdentifier.class);
    }

    @Override
    protected OrganizationIdentifierDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getForOrganization(Integer organizationId) {
        return baseObjectDAO.getForOrganization(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getForOrganizations(List<Integer> organizationIds) {
        return baseObjectDAO.getForOrganizations(organizationIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> getByLabelAndValue(String label, String value) {
        return baseObjectDAO.getByLabelAndValue(label, value);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationIdentifier> searchByValue(String text, int limit) {
        return baseObjectDAO.searchByValue(text, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctLabels() {
        return baseObjectDAO.getDistinctLabels();
    }

    @Override
    @Transactional
    public List<OrganizationIdentifier> replaceForOrganization(Integer organizationId,
            List<OrganizationIdentifier> wanted, String sysUserId) {
        baseObjectDAO.deleteForOrganization(organizationId);
        List<OrganizationIdentifier> stored = new ArrayList<>();
        for (OrganizationIdentifier identifier : wanted) {
            OrganizationIdentifier fresh = new OrganizationIdentifier(organizationId, identifier.getLabel().trim(),
                    identifier.getValue().trim(), identifier.isReporting());
            fresh.setSysUserId(sysUserId);
            fresh.setId(baseObjectDAO.insert(fresh));
            stored.add(fresh);
        }
        return stored;
    }
}
