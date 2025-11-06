package org.openelisglobal.notification.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notification.dao.NotificationPayloadTemplateDAO;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPayloadTemplateServiceImpl
        extends AuditableBaseObjectServiceImpl<NotificationPayloadTemplate, Integer>
        implements NotificationPayloadTemplateService {

    @Autowired
    private NotificationPayloadTemplateDAO baseDAO;

    public NotificationPayloadTemplateServiceImpl() {
        super(NotificationPayloadTemplate.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<NotificationPayloadTemplate, Integer> getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    public NotificationPayloadTemplate getSystemDefaultPayloadTemplateForType(NotificationPayloadType type) {
        return baseDAO.getSystemDefaultPayloadTemplateForType(type);
    }

    @Override
    @Transactional
    public void updatePayloadTemplateMessagesAndSubject(NotificationPayloadTemplate newPayloadTemplate,
            String sysUserId) {
        NotificationPayloadTemplate oldTemplate;
        if (newPayloadTemplate.getId() == null) {
            oldTemplate = newPayloadTemplate;
        } else {
            oldTemplate = get(newPayloadTemplate.getId());
            oldTemplate.setSubjectTemplate(newPayloadTemplate.getSubjectTemplate());
            oldTemplate.setMessageTemplate(newPayloadTemplate.getMessageTemplate());
        }
        oldTemplate.setSysUserId(sysUserId);
        save(oldTemplate);
    }
}
