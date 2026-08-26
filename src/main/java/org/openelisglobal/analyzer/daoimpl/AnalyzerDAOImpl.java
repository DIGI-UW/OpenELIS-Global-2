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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.analyzer.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.service.AnalyzerTestCapability;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerDAOImpl extends BaseDAOImpl<Analyzer, String> implements AnalyzerDAO {

    public AnalyzerDAOImpl() {
        super(Analyzer.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            String hql = "FROM Analyzer a WHERE a.name = :name";
            Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
            query.setParameter("name", name.trim());
            Analyzer result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple Analyzers found for name: " + name, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analyzer> findAllWithBindings() {
        String hql = "SELECT a FROM Analyzer a " + "LEFT JOIN FETCH a.siteBindingRevision revision "
                + "LEFT JOIN FETCH revision.siteBinding binding " + "LEFT JOIN FETCH binding.profileBinding";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByIdWithBinding(String id) {
        String hql = "SELECT a FROM Analyzer a " + "LEFT JOIN FETCH a.siteBindingRevision revision "
                + "LEFT JOIN FETCH revision.siteBinding binding " + "LEFT JOIN FETCH binding.profileBinding "
                + "WHERE a.id = :id";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        query.setParameter("id", id);
        Analyzer result = query.uniqueResult();
        return Optional.ofNullable(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId) {
        if (bridgeConnectionId == null || bridgeConnectionId.isBlank()) {
            return Optional.empty();
        }
        String hql = "SELECT a FROM Analyzer a " + "JOIN FETCH a.siteBindingRevision revision "
                + "JOIN FETCH revision.siteBinding binding " + "JOIN FETCH binding.profileBinding "
                + "WHERE a.bridgeConnectionId = :connectionId";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        query.setParameter("connectionId", bridgeConnectionId.trim());
        return Optional.ofNullable(query.uniqueResult());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerTestCapability> findCapabilitiesByTestId(String testId) {
        String hql = "SELECT new org.openelisglobal.analyzer.service.AnalyzerTestCapability("
                + "a.id, a.name, mapping.id.sourceRowKey) " + "FROM Analyzer a, AnalyzerSiteBindingTest mapping "
                + "WHERE a.siteBindingRevision = mapping.siteBindingRevision " + "AND mapping.testId = :testId "
                + "AND mapping.mappingState = :mappedState " + "ORDER BY lower(a.name), mapping.id.sourceRowKey";
        Query<AnalyzerTestCapability> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerTestCapability.class);
        query.setParameter("testId", testId);
        query.setParameter("mappedState", AnalyzerSiteBindingMappingState.BOUND);
        return query.getResultList();
    }
}
