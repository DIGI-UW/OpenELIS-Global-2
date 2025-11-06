package org.openelisglobal.renametestsection.service;

import java.util.List;
import org.hibernate.Hibernate;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.renametestsection.dao.RenameTestSectionDAO;
import org.openelisglobal.renametestsection.valueholder.RenameTestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenameTestSectionServiceImpl extends AuditableBaseObjectServiceImpl<RenameTestSection, String>
        implements RenameTestSectionService {
    @Autowired
    protected RenameTestSectionDAO baseObjectDAO;

    RenameTestSectionServiceImpl() {
        super(RenameTestSection.class);
    }

    @Override
    protected RenameTestSectionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(RenameTestSection testSection) {
        getBaseObjectDAO().getData(testSection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameTestSection> getTestSections(String filter) {
        return getBaseObjectDAO().getTestSections(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public RenameTestSection getTestSectionByName(RenameTestSection testSection) {
        return getBaseObjectDAO().getTestSectionByName(testSection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameTestSection> getPageOfTestSections(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTestSections(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestSectionCount() {
        return getBaseObjectDAO().getTotalTestSectionCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameTestSection> getAllTestSections() {
        return getBaseObjectDAO().getAllTestSections();
    }

    @Override
    @Transactional(readOnly = true)
    public RenameTestSection getTestSectionById(String id) {
        return getBaseObjectDAO().getTestSectionById(id);
    }

    @Override
    @Transactional
    public String insert(RenameTestSection renameTestSection) {
        if (duplicateTestSectionExists(renameTestSection)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + renameTestSection.getTestSectionName());
        }
        return super.insert(renameTestSection);
    }

    @Override
    @Transactional
    public RenameTestSection save(RenameTestSection renameTestSection) {
        if (duplicateTestSectionExists(renameTestSection)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + renameTestSection.getTestSectionName());
        }
        return super.save(renameTestSection);
    }

    @Override
    @Transactional
    public RenameTestSection update(RenameTestSection renameTestSection) {
        if (duplicateTestSectionExists(renameTestSection)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + renameTestSection.getTestSectionName());
        }
        return super.update(renameTestSection);
    }

    private boolean duplicateTestSectionExists(RenameTestSection renameTestSection) {
        return baseObjectDAO.duplicateTestSectionExists(renameTestSection);
    }

    @Override
    @Transactional(readOnly = true)
    public Localization getLocalizationForRenameTestSection(String id) {
        RenameTestSection renameTestSection = getTestSectionById(id);
        Localization localization = renameTestSection != null ? renameTestSection.getLocalization() : null;
        Hibernate.initialize(localization);
        return localization;
    }
}
