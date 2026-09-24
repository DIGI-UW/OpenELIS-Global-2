package org.openelisglobal.configuration.service;

import java.sql.Timestamp;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.configuration.dao.ConfigurationImportRunDAO;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigurationImportRunServiceImpl extends BaseObjectServiceImpl<ConfigurationImportRun, String>
        implements ConfigurationImportRunService {

    @Autowired
    protected ConfigurationImportRunDAO baseObjectDAO;

    public ConfigurationImportRunServiceImpl() {
        super(ConfigurationImportRun.class);
    }

    @Override
    protected ConfigurationImportRunDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfigurationImportRun start(String source, String sysUserId) {
        ConfigurationImportRun run = new ConfigurationImportRun();
        run.setSource(source);
        run.setStatus(ConfigurationImportRun.STATUS_RUNNING);
        run.setStartedAt(new Timestamp(System.currentTimeMillis()));
        run.setSystemUserId(Integer.valueOf(sysUserId));
        run.setSysUserId(sysUserId);
        insert(run);
        ImportRunContext.clear();
        ImportRunContext.setRunId(run.getId());
        return run;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(ConfigurationImportRun run, String summary, boolean failed) {
        try {
            ConfigurationImportRun current = get(run.getId());
            if (current != null) {
                current.setStatus(
                        failed ? ConfigurationImportRun.STATUS_FAILED : ConfigurationImportRun.STATUS_COMPLETED);
                current.setFinishedAt(new Timestamp(System.currentTimeMillis()));
                current.setSummary(summary);
                current.setSysUserId(run.getSysUserId());
                update(current);
            }
        } finally {
            ImportRunContext.clear();
        }
    }
}
