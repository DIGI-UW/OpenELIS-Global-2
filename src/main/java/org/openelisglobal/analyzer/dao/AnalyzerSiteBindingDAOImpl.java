package org.openelisglobal.analyzer.dao;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingDAOImpl extends BaseDAOImpl<AnalyzerSiteBinding, String>
        implements AnalyzerSiteBindingDAO {

    public AnalyzerSiteBindingDAOImpl() {
        super(AnalyzerSiteBinding.class);
    }

    @Override
    public Optional<AnalyzerSiteBinding> findByIdForUpdate(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional
                .ofNullable(entityManager.find(AnalyzerSiteBinding.class, id.trim(), LockModeType.PESSIMISTIC_WRITE));
    }
}
