package org.openelisglobal.configuration.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ConfigurationImportRunDAOImpl extends BaseDAOImpl<ConfigurationImportRun, String>
        implements ConfigurationImportRunDAO {

    public ConfigurationImportRunDAOImpl() {
        super(ConfigurationImportRun.class);
    }
}
