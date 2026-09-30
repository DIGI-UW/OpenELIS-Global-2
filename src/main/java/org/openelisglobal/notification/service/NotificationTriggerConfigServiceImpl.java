package org.openelisglobal.notification.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notification.dao.NotificationTriggerConfigDAO;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationTriggerConfigServiceImpl extends
        AuditableBaseObjectServiceImpl<NotificationTriggerConfig, Integer> implements NotificationTriggerConfigService {

    @Autowired
    private NotificationTriggerConfigDAO baseDAO;

    public NotificationTriggerConfigServiceImpl() {
        super(NotificationTriggerConfig.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<NotificationTriggerConfig, Integer> getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTriggerConfig> getAllConfigs() {
        return getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTriggerConfig> getByEventCode(String eventCode) {
        return baseDAO.findByEventCode(eventCode);
    }

    @Override
    @Transactional
    public NotificationTriggerConfig saveConfig(NotificationTriggerConfig incoming, String sysUserId) {
        NotificationTriggerConfig target;
        if (incoming.getId() != null) {
            target = get(incoming.getId());
        } else if (incoming.getEventCode() != null) {
            target = baseDAO.findByEventCode(incoming.getEventCode()).orElseGet(NotificationTriggerConfig::new);
            target.setEventCode(incoming.getEventCode());
        } else {
            throw new IllegalArgumentException("NotificationTriggerConfig requires either id or eventCode");
        }
        target.setEnabled(incoming.isEnabled());
        target.setChannels(incoming.getChannels() == null ? new HashSet<>() : new HashSet<>(incoming.getChannels()));
        target.setRecipientTypes(
                incoming.getRecipientTypes() == null ? new HashSet<>() : new HashSet<>(incoming.getRecipientTypes()));
        if (incoming.getPayloadTemplate() != null) {
            target.setPayloadTemplate(incoming.getPayloadTemplate());
        }
        target.setSysUserId(sysUserId);
        return save(target);
    }

    @Override
    @Transactional
    public void saveAll(List<NotificationTriggerConfig> incoming, String sysUserId) {
        if (incoming == null) {
            return;
        }
        for (NotificationTriggerConfig cfg : incoming) {
            saveConfig(cfg, sysUserId);
        }
    }
}
