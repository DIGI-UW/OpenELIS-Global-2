package org.openelisglobal.configuration.service;

import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.util.TestDescriptionNormalizer;
import org.openelisglobal.configuration.dao.ReferenceAliasDAO;
import org.openelisglobal.configuration.valueholder.ReferenceAlias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceAliasServiceImpl extends BaseObjectServiceImpl<ReferenceAlias, String>
        implements ReferenceAliasService {

    @Autowired
    protected ReferenceAliasDAO baseObjectDAO;

    public ReferenceAliasServiceImpl() {
        super(ReferenceAlias.class);
    }

    @Override
    protected ReferenceAliasDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String referenceType, String name) {
        String normalized = TestDescriptionNormalizer.normalizeText(name);
        if (normalized.isEmpty()) {
            return null;
        }
        ReferenceAlias alias = baseObjectDAO.getByTypeAndAlias(referenceType, normalized);
        return alias == null ? null : alias.getTargetId();
    }

    @Override
    @Transactional
    public ReferenceAlias remember(String referenceType, String name, String targetId, String sysUserId) {
        String normalized = TestDescriptionNormalizer.normalizeText(name);
        ReferenceAlias alias = baseObjectDAO.getByTypeAndAlias(referenceType, normalized);
        if (alias == null) {
            alias = new ReferenceAlias();
            alias.setReferenceType(referenceType);
            alias.setAlias(normalized);
        }
        alias.setTargetId(targetId);
        alias.setSystemUserId(Integer.valueOf(sysUserId));
        alias.setSysUserId(sysUserId);
        if (alias.getLastupdated() == null) {
            insert(alias);
        } else {
            update(alias);
        }
        return alias;
    }
}
