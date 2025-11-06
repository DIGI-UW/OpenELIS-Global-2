package org.openelisglobal.analyzerimport.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.dao.AnalyzerTestMappingDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMappingPK;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerTestMappingServiceImpl
        extends AuditableBaseObjectServiceImpl<AnalyzerTestMapping, AnalyzerTestMappingPK>
        implements AnalyzerTestMappingService {
    @Autowired
    protected AnalyzerTestMappingDAO baseObjectDAO;

    AnalyzerTestMappingServiceImpl() {
        super(AnalyzerTestMapping.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected AnalyzerTestMappingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<AnalyzerTestMapping> getAllForAnalyzer(String analyzerId) {
        return baseObjectDAO.getAllForAnalyzer(analyzerId);
    }
}
