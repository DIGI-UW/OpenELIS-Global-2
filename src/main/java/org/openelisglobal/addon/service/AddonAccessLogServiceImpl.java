package org.openelisglobal.addon.service;

import java.time.Instant;
import java.util.List;
import org.openelisglobal.addon.dao.AddonAccessLogDAO;
import org.openelisglobal.addon.dao.AddonAccessLogDAO.Entry;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AddonAccessLogServiceImpl implements AddonAccessLogService {

    @Autowired
    private AddonAccessLogDAO dao;
    @Autowired
    private AddonSettings settings;

    @Override
    public void record(Entry entry) {
        try {
            dao.insert(entry);
        } catch (RuntimeException e) {
            // the answer has been sent already; a failed record is reported, not hidden
            LogEvent.logError(getClass().getSimpleName(), "record",
                    "could not record add-on API access by " + entry.addonId() + " to " + entry.endpoint());
        }
    }

    @Override
    public List<Entry> find(String addonId, Instant from, Instant to, String actor, String subject, int limit) {
        return dao.find(addonId, from, to, actor, subject, limit);
    }

    @Scheduled(fixedDelayString = "${addons.accessLog.purge-ms:3600000}", initialDelayString = "${addons.accessLog.purge-initial-delay-ms:60000}")
    public void purge() {
        try {
            dao.purgeOlderThan(settings.accessLogRetentionDays());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
    }
}
