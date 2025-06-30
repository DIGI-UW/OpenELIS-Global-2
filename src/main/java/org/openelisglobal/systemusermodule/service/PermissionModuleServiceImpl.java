package org.openelisglobal.systemusermodule.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionModuleServiceImpl implements PermissionModuleService<PermissionModule> {

    @Autowired
    protected SystemUserModuleService systemUserService;

    @Autowired
    protected RoleModuleService roleModuleService;

    @SuppressWarnings("rawtypes")
    protected PermissionModuleService getActivePermissionModule() {
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equals("USER")) {
            return systemUserService;
        } else {
            return roleModuleService;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionModule get(String id) {
        return (PermissionModule) getActivePermissionModule().get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Optional<PermissionModule> getMatch(String propertyName, Object propertyValue) {
        return getActivePermissionModule().getMatch(propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Optional<PermissionModule> getMatch(Map<String, Object> propertyValues) {
        return getActivePermissionModule().getMatch(propertyValues);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAll() {
        return getActivePermissionModule().getAll();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatching(String propertyName, Object propertyValue) {
        return getActivePermissionModule().getAllMatching(propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatching(Map<String, Object> propertyValues) {
        return getActivePermissionModule().getAllMatching(propertyValues);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllOrdered(String orderProperty, boolean descending) {
        return getActivePermissionModule().getAllOrdered(orderProperty, descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllOrdered(List<String> orderProperties, boolean descending) {
        return getActivePermissionModule().getAllOrdered(orderProperties, descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatchingOrdered(String propertyName, Object propertyValue, String orderProperty,
            boolean descending) {
        return getActivePermissionModule().getAllMatchingOrdered(propertyName, propertyValue, orderProperty,
                descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatchingOrdered(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending) {
        return getActivePermissionModule().getAllMatchingOrdered(propertyName, propertyValue, orderProperties,
                descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty,
            boolean descending) {
        return getActivePermissionModule().getAllMatchingOrdered(propertyValues, orderProperty, descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllMatchingOrdered(Map<String, Object> propertyValues,
            List<String> orderProperties, boolean descending) {
        return getActivePermissionModule().getAllMatchingOrdered(propertyValues, orderProperties, descending);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getPage(int pageNumber) {
        return getActivePermissionModule().getPage(pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingPage(String propertyName, Object propertyValue, int pageNumber) {
        return getActivePermissionModule().getMatchingPage(propertyName, propertyValue, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingPage(Map<String, Object> propertyValues, int pageNumber) {
        return getActivePermissionModule().getMatchingPage(propertyValues, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getOrderedPage(String orderProperty, boolean descending, int pageNumber) {
        return getActivePermissionModule().getOrderedPage(orderProperty, descending, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getOrderedPage(List<String> orderProperties, boolean descending, int pageNumber) {
        return getActivePermissionModule().getOrderedPage(orderProperties, descending, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingOrderedPage(String propertyName, Object propertyValue,
            String orderProperty, boolean descending, int pageNumber) {
        return getActivePermissionModule().getMatchingOrderedPage(propertyName, propertyValue, orderProperty,
                descending, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingOrderedPage(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending, int pageNumber) {
        return getActivePermissionModule().getMatchingOrderedPage(propertyName, propertyValue, orderProperties,
                descending, pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty,
            boolean descending, int pageNumber) {
        return getActivePermissionModule().getMatchingOrderedPage(propertyValues, orderProperty, descending,
                pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getMatchingOrderedPage(Map<String, Object> propertyValues,
            List<String> orderProperties, boolean descending, int pageNumber) {
        return getActivePermissionModule().getMatchingOrderedPage(propertyValues, orderProperties, descending,
                pageNumber);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public String insert(PermissionModule baseObject) {
        return (String) getActivePermissionModule().insert(baseObject);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<String> insertAll(List<PermissionModule> baseObjects) {
        return getActivePermissionModule().insertAll(baseObjects);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public PermissionModule save(PermissionModule baseObject) {
        return (PermissionModule) getActivePermissionModule().save(baseObject);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<PermissionModule> saveAll(List<PermissionModule> baseObjects) {
        return getActivePermissionModule().saveAll(baseObjects);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public PermissionModule update(PermissionModule baseObject) {
        return (PermissionModule) getActivePermissionModule().update(baseObject);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<PermissionModule> updateAll(List<PermissionModule> baseObjects) {
        return getActivePermissionModule().updateAll(baseObjects);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public void delete(PermissionModule baseObject) {
        getActivePermissionModule().delete(baseObject);
    }

    @Override
    @Transactional
    public void delete(String id, String sysUserId) {
        getActivePermissionModule().delete(id, sysUserId);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public void deleteAll(List<PermissionModule> baseObjects) {
        getActivePermissionModule().deleteAll(baseObjects);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public void deleteAll(List<String> ids, String sysUserId) {
        getActivePermissionModule().deleteAll(ids, sysUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCount() {
        return getActivePermissionModule().getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountMatching(String propertyName, Object propertyValue) {
        return getActivePermissionModule().getCountMatching(propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Integer getCountMatching(Map<String, Object> propertyValues) {
        return getActivePermissionModule().getCountMatching(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountLike(String propertyName, String propertyValue) {
        return getActivePermissionModule().getCountLike(propertyName, propertyValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Integer getCountLike(Map<String, String> propertyValues) {
        return getActivePermissionModule().getCountLike(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionModule getNext(String id) {
        return (PermissionModule) getActivePermissionModule().getNext(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionModule getPrevious(String id) {
        return (PermissionModule) getActivePermissionModule().getPrevious(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNext(String id) {
        return getActivePermissionModule().hasNext(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPrevious(String id) {
        return getActivePermissionModule().hasPrevious(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public List<PermissionModule> getAllPermissionModulesByAgentId(int agentId) {
        return getActivePermissionModule().getAllMatching("role.id", agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(PermissionModule permissionModule) {
        getActivePermissionModule().getData(permissionModule);
    }

    @Override
    @Transactional(readOnly = true)
    public List getAllPermissionModules() {
        return getActivePermissionModule().getAllPermissionModules();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPermissionModuleCount() {
        return getActivePermissionModule().getTotalPermissionModuleCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List getPageOfPermissionModules(int startingRecNo) {
        return getActivePermissionModule().getPageOfPermissionModules(startingRecNo);
    }

    @Override
    public boolean doesUserHaveAnyModules(int userId) {
        return getActivePermissionModule().doesUserHaveAnyModules(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllPermittedPagesFromAgentId(int roleId) {
        return getActivePermissionModule().getAllPermittedPagesFromAgentId(roleId);
    }
}
