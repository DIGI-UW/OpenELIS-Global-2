package org.openelisglobal.common.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.valueholder.BaseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public abstract class AuditableBaseObjectServiceImpl<T extends BaseObject<PK>, PK extends Serializable>
        extends BaseObjectServiceImpl<T, PK> implements AuditableBaseObjectService {

    @Autowired
    protected AuditTrailService auditTrailService;

    protected boolean auditTrailLog = false;

    public AuditableBaseObjectServiceImpl(Class<T> clazz) {
        super(clazz);
    }

    @Override
    @Transactional
    public PK insert(T baseObject) {
        PK id = super.insert(baseObject);
        if (auditTrailLog) {
            auditTrailService.saveNewHistory(baseObject, baseObject.getSysUserId(), getBaseObjectDAO().getTableName());
        }
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
        return update(baseObject, IActionConstants.AUDIT_TRAIL_UPDATE);
    }

    protected T update(T baseObject, String auditTrailType) {
        if (auditTrailLog) {
            T oldObject = getBaseObjectDAO().get(baseObject.getId())
                    .orElseThrow(() -> new ObjectNotFoundException(baseObject.getId(), classType.getName()));
            getBaseObjectDAO().evict(oldObject);
            auditTrailService.saveHistory(baseObject, oldObject, baseObject.getSysUserId(), auditTrailType,
                    getBaseObjectDAO().getTableName());
        }
        return super.update(baseObject);
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

    // used for "deleting" an object but operation is actually an update
    protected void updateDelete(T baseObject) {
        update(baseObject, IActionConstants.AUDIT_TRAIL_DELETE);
    }

    @Override
    @Transactional
    public void delete(T baseObject) {
        if (auditTrailLog) {
            auditTrailService.saveHistory(null, baseObject, baseObject.getSysUserId(),
                    IActionConstants.AUDIT_TRAIL_DELETE, getBaseObjectDAO().getTableName());
        }
        // this is so we can make sure entity is managed before it is deleted as calling
        // delete on an unmanaged object will mean it is not removed from the database
        super.delete(baseObject);
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

    protected void disableLogging() {
        this.auditTrailLog = false;
    }
}
