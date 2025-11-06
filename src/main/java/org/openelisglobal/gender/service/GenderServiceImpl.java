package org.openelisglobal.gender.service;

import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.gender.dao.GenderDAO;
import org.openelisglobal.gender.valueholder.Gender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenderServiceImpl extends AuditableBaseObjectServiceImpl<Gender, Integer> implements GenderService {
    @Autowired
    protected GenderDAO baseObjectDAO;

    GenderServiceImpl() {
        super(Gender.class);
    }

    @Override
    protected GenderDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public Integer insert(Gender gender) {
        if (getBaseObjectDAO().duplicateGenderExists(gender)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + gender.getGenderType());
        }
        return super.insert(gender);
    }

    @Override
    public Gender save(Gender gender) {
        if (getBaseObjectDAO().duplicateGenderExists(gender)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + gender.getGenderType());
        }
        return super.save(gender);
    }

    @Override
    public Gender update(Gender gender) {
        if (getBaseObjectDAO().duplicateGenderExists(gender)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + gender.getGenderType());
        }
        return super.update(gender);
    }

    @Override
    @Transactional(readOnly = true)
    public Gender getGenderByType(String type) {
        return getMatch("genderType", type).orElse(null);
    }
}
