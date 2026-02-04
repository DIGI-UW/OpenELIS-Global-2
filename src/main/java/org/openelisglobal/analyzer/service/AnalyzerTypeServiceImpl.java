/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerTypeDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerTypeServiceImpl extends AuditableBaseObjectServiceImpl<AnalyzerType, String>
        implements AnalyzerTypeService {

    @Autowired
    protected AnalyzerTypeDAO baseObjectDAO;

    @Autowired
    private AnalyzerService analyzerService;

    AnalyzerTypeServiceImpl() {
        super(AnalyzerType.class);
    }

    @Override
    protected AnalyzerTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerType getAnalyzerTypeByName(String name) {
        return getMatch("name", name).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerType> getByPluginClassName(String pluginClassName) {
        return getMatch("pluginClassName", pluginClassName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerType> getAllActiveTypes() {
        return getAllMatching("active", true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerType> getGenericPluginTypes() {
        return getAllMatching("genericPlugin", true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerType> findMatchingType(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return Optional.empty();
        }

        List<AnalyzerType> activeTypes = getAllActiveTypes();
        for (AnalyzerType type : activeTypes) {
            if (type.matchesIdentifier(identifier)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analyzer> getInstancesForType(String analyzerTypeId) {
        AnalyzerType type = get(analyzerTypeId);
        if (type == null) {
            return List.of();
        }
        return type.getInstances();
    }

    @Override
    @Transactional
    public Analyzer getOrCreateDefaultInstance(AnalyzerType analyzerType) {
        // Check if any instance exists
        List<Analyzer> instances = analyzerType.getInstances();
        if (!instances.isEmpty()) {
            return instances.get(0);
        }

        // Create default instance
        Analyzer defaultInstance = new Analyzer();
        defaultInstance.setName(analyzerType.getName());
        defaultInstance.setDescription("Default instance for " + analyzerType.getName());
        defaultInstance.setAnalyzerType(analyzerType);
        defaultInstance.setActive(true);
        defaultInstance.setSysUserId("1");

        analyzerService.insert(defaultInstance);
        return defaultInstance;
    }
}
