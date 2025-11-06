package org.openelisglobal.systemuser.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.systemuser.dao.SystemUserDAO;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemUserServiceImpl extends AuditableBaseObjectServiceImpl<SystemUser, String>
        implements SystemUserService {
    @Autowired
    protected SystemUserDAO baseObjectDAO;

    SystemUserServiceImpl() {
        super(SystemUser.class);
    }

    @Override
    protected SystemUserDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public void delete(SystemUser systemUser) {
        SystemUser oldData = get(systemUser.getId());
        oldData.setIsActive(IActionConstants.NO);
        oldData.setSysUserId(systemUser.getSysUserId());
        updateDelete(oldData);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemUser systemUser) {
        getBaseObjectDAO().getData(systemUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUser> getPageOfSystemUsers(int startingRecNo) {
        return getBaseObjectDAO().getPageOfSystemUsers(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUser> getAllSystemUsers() {
        return getBaseObjectDAO().getAllSystemUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSystemUserCount() {
        return getBaseObjectDAO().getTotalSystemUserCount();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemUser getDataForLoginUser(String name) {
        return getBaseObjectDAO().getDataForLoginUser(name);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemUser getUserById(String userId) {
        return getBaseObjectDAO().getUserById(userId);
    }

    @Override
    public String insert(SystemUser systemUser) {
        if (duplicateSystemUserExists(systemUser)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemUser.getFirstName()
                    + IActionConstants.BLANK + systemUser.getFirstName());
        }
        return super.insert(systemUser);
    }

    @Override
    public SystemUser save(SystemUser systemUser) {
        if (duplicateSystemUserExists(systemUser)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemUser.getFirstName()
                    + IActionConstants.BLANK + systemUser.getFirstName());
        }
        return super.save(systemUser);
    }

    @Override
    public SystemUser update(SystemUser systemUser) {
        if (duplicateSystemUserExists(systemUser)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + systemUser.getFirstName()
                    + IActionConstants.BLANK + systemUser.getFirstName());
        }
        return super.update(systemUser);
    }

    private boolean duplicateSystemUserExists(SystemUser systemUser) {
        return baseObjectDAO.duplicateSystemUserExists(systemUser);
    }

    @Override
    public List<SystemUser> getPagesOfSearchedUsers(int startRecNo, String searchString) {
        List<SystemUser> systemUsers = new ArrayList<>();
        systemUsers = baseObjectDAO.getLikePage("loginName", searchString, startRecNo);
        if (systemUsers.isEmpty()) {
            systemUsers = baseObjectDAO.getLikePage("firstName", searchString, startRecNo);
        }
        if (systemUsers.isEmpty()) {
            systemUsers = baseObjectDAO.getLikePage("lastName", searchString, startRecNo);
        }
        return systemUsers;
    }

    @Override
    public Integer getTotalSearchedUserCount(String searchString) {
        Integer count = getCountLike("loginName", searchString);
        if (ObjectUtils.defaultIfNull(count, 0) == 0) {
            count = getCountLike("firstName", searchString);
        }
        if (ObjectUtils.defaultIfNull(count, 0) == 0) {
            count = getCountLike("lastName", searchString);
        }
        return ObjectUtils.defaultIfNull(count, 0);
    }
}
