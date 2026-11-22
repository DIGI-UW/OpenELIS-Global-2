package org.openelisglobal.notification.service;

import java.util.List;
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

    /** A template named a merge field its slot does not allow. */
    public static class DisallowedFieldException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        private final List<String> fields;

        public DisallowedFieldException(List<String> fields) {
            super("Merge field not allowed in this template: " + String.join(", ", fields));
            this.fields = fields;
        }

        public List<String> getFields() {
            return fields;
        }
    }

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

    /**
     * Stores the subject and message after checking every {@code [field]} they name
     * against the slot's allowed merge fields; a disallowed one, and in particular
     * any patient identifier, refuses the save with the field named.
     */
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
        validate(oldTemplate);
        oldTemplate.setSysUserId(sysUserId);
        save(oldTemplate);
    }

    static void validate(NotificationPayloadTemplate template) {
        if (template.getType() == null) {
            return;
        }
        List<String> disallowed = EventVariableRegistry.disallowedTokens(template.getType(),
                template.getSubjectTemplate());
        for (String token : EventVariableRegistry.disallowedTokens(template.getType(), template.getMessageTemplate())) {
            if (!disallowed.contains(token)) {
                disallowed.add(token);
            }
        }
        if (!disallowed.isEmpty()) {
            throw new DisallowedFieldException(disallowed);
        }
    }
}
