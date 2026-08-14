package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerTypeCatalogServiceImpl implements AnalyzerTypeCatalogService {

    public AnalyzerTypeCatalogServiceImpl(AnalyzerProfileCatalogClient profileCatalogClient,
            AnalyzerSiteBindingRevisionDAO revisionDAO, AnalyzerSiteBindingTestDAO testDAO, AnalyzerDAO analyzerDAO) {
    }

    @Override
    public List<AnalyzerTypeCatalogSummary> list(AnalyzerProfileCatalogFilter filter) {
        return List.of();
    }
}
