package org.openelisglobal.common.service;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.valueholder.BaseObject;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseObjectServiceImpl<T extends BaseObject<PK>, PK extends Serializable>
        implements BaseObjectService<T, PK> {

    protected final Class<T> classType;

    protected List<String> defaultSortOrder = new ArrayList<>(Arrays.asList("id"));

    public BaseObjectServiceImpl(Class<T> clazz) {
        classType = clazz;
    }

    protected abstract BaseDAO<T, PK> getBaseObjectDAO();

    @Override
    @Transactional(readOnly = true)
    public T get(PK id) {
        return getBaseObjectDAO().get(id).orElseThrow(() -> new ObjectNotFoundException(id, classType.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> getMatch(String propertyName, Object propertyValue) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        return getMatch(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> getMatch(Map<String, Object> propertyValues) {
        List<T> matches = getAllMatching(propertyValues);

        if (matches.isEmpty() || matches.size() > 1) {
            return Optional.empty();
        } else {
            return Optional.of(matches.get(0));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAll() {
        return getBaseObjectDAO().getAllOrdered(defaultSortOrder, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatching(String propertyName, Object propertyValue) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyName, propertyValue, defaultSortOrder, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatching(Map<String, Object> propertyValues) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyValues, defaultSortOrder, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllOrdered(String orderProperty, boolean descending) {
        return getBaseObjectDAO().getAllOrdered(orderProperty, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllOrdered(List<String> orderProperties, boolean descending) {
        return getBaseObjectDAO().getAllOrdered(orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, String orderProperty,
            boolean descending) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyName, propertyValue, orderProperty, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyName, propertyValue, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty, boolean descending) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyValues, orderProperty, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getAllMatchingOrdered(Map<String, Object> propertyValues, List<String> orderProperties,
            boolean descending) {
        return getBaseObjectDAO().getAllMatchingOrdered(propertyValues, orderProperties, descending);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getPage(int pageNumber) {
        return getBaseObjectDAO().getOrderedPage(defaultSortOrder, false, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingPage(String propertyName, Object propertyValue, int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyName, propertyValue, defaultSortOrder, false,
                pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingPage(Map<String, Object> propertyValues, int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyValues, defaultSortOrder, false, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getOrderedPage(String orderProperty, boolean descending, int pageNumber) {
        return getBaseObjectDAO().getOrderedPage(orderProperty, descending, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getOrderedPage(List<String> orderProperties, boolean descending, int pageNumber) {
        return getBaseObjectDAO().getOrderedPage(orderProperties, descending, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, String orderProperty,
            boolean descending, int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyName, propertyValue, orderProperty, descending,
                pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(String propertyName, Object propertyValue, List<String> orderProperties,
            boolean descending, int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyName, propertyValue, orderProperties, descending,
                pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty, boolean descending,
            int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyValues, orderProperty, descending, pageNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> getMatchingOrderedPage(Map<String, Object> propertyValues, List<String> orderProperties,
            boolean descending, int pageNumber) {
        return getBaseObjectDAO().getMatchingOrderedPage(propertyValues, orderProperties, descending, pageNumber);
    }

    @Override
    @Transactional
    public PK insert(T baseObject) {
        PK id = getBaseObjectDAO().insert(baseObject);
        baseObject.setId(id);
        return id;
    }

    @Override
    @Transactional
    public List<PK> insertAll(List<T> baseObjects) {
        List<PK> ids = new ArrayList<>();
        for (T baseObject : baseObjects) {
            ids.add(insert(baseObject));
        }
        return ids;
    }

    @Override
    @Transactional
    public T save(T baseObject) {

        if ((baseObject.getId() instanceof String && GenericValidator.isBlankOrNull((String) baseObject.getId()))
                || null == baseObject.getId()) {
            return get(insert(baseObject));
        } else {
            return update(baseObject);
        }
    }

    @Override
    @Transactional
    public List<T> saveAll(List<T> baseObjects) {
        List<T> resultObjects = new ArrayList<>();
        for (T baseObject : baseObjects) {
            resultObjects.add(save(baseObject));
        }
        return resultObjects;
    }

    @Override
    @Transactional
    public T update(T baseObject) {
        if (baseObject.getLastupdated() == null) {
            // this is done so detached objects that are being updated don't do an insert
            // operation
            // when their version value is null (hiberante behaviour)
            LogEvent.logWarn(this.getClass().getSimpleName(), "update",
                    "running update on an object with a missing version field can result in unintended"
                            + " inserts instead of updates");
            LogEvent.logWarn(this.getClass().getSimpleName(), "update", "setting lastUpdated to now for object: "
                    + baseObject.getClass().getSimpleName() + " with id: " + baseObject.getId());
            baseObject.setLastupdated(Timestamp.from(Instant.now()));
        }
        return getBaseObjectDAO().update(baseObject);
    }

    @Override
    @Transactional
    public List<T> updateAll(List<T> baseObjects) {
        List<T> resultObjects = new ArrayList<>();
        for (T baseObject : baseObjects) {
            resultObjects.add(update(baseObject));
        }
        return resultObjects;
    }

    @Override
    @Transactional
    public void delete(T baseObject) {
        // this is so we can make sure entity is managed before it is deleted as calling
        // delete on an unmanaged object will mean it is not removed from the database
        getBaseObjectDAO().delete(getBaseObjectDAO().get(baseObject.getId())
                .orElseThrow(() -> new ObjectNotFoundException(baseObject.getId(), classType.getName())));
    }

    @Override
    @Transactional
    public void delete(PK id, String sysUserId) {
        T oldObject = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, classType.getName()));
        oldObject.setSysUserId(sysUserId);
        delete(oldObject);
    }

    @Override
    @Transactional
    public void deleteAll(List<T> baseObjects) {
        for (T baseObject : baseObjects) {
            delete(baseObject);
        }
    }

    @Override
    @Transactional
    public void deleteAll(List<PK> ids, String sysUserId) {
        for (PK id : ids) {
            delete(id, sysUserId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCount() {
        return getBaseObjectDAO().getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountMatching(String propertyName, Object propertyValue) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        return getCountMatching(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountMatching(Map<String, Object> propertyValues) {
        return getBaseObjectDAO().getAllMatching(propertyValues).size();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountLike(String propertyName, String propertyValue) {
        Map<String, String> propertyValues = new HashMap<>();
        propertyValues.put(propertyName, propertyValue);
        return getCountLike(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCountLike(Map<String, String> propertyValues) {
        return getBaseObjectDAO().getAllLike(propertyValues).size();
    }

    @Override
    @Transactional(readOnly = true)
    public T getNext(String id) {
        try {
            return getBaseObjectDAO().getNext(id).orElse(classType.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            LogEvent.logError("Could not create new Instance for " + classType.getName(), e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public T getPrevious(String id) {
        try {
            return getBaseObjectDAO().getPrevious(id).orElse(classType.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            LogEvent.logError("Could not create new Instance for " + classType.getName(), e);
            throw new LIMSRuntimeException(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNext(String id) {
        return getBaseObjectDAO().getNext(id).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPrevious(String id) {
        return getBaseObjectDAO().getPrevious(id).isPresent();
    }
}
