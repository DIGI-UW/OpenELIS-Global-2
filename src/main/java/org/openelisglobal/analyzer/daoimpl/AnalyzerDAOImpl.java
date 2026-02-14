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
import org.openelisglobal.analyzer.valueholder.Analyzer;
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
    public Optional<Analyzer> findByIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            String hql = "FROM Analyzer a WHERE a.ipAddress = :ipAddress";
            Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
            query.setParameter("ipAddress", ipAddress.trim());
            Analyzer result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple Analyzers found for IP address: " + ipAddress, e);
        }
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
    public List<Analyzer> findGenericAnalyzersWithPatterns() {
        String hql = "SELECT a FROM Analyzer a " + "JOIN FETCH a.analyzerType at " + "WHERE at.genericPlugin = true "
                + "AND a.identifierPattern IS NOT NULL";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analyzer> findAllWithTypes() {
        String hql = "SELECT a FROM Analyzer a LEFT JOIN FETCH a.analyzerType";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Analyzer> findByIdWithType(String id) {
        String hql = "SELECT a FROM Analyzer a LEFT JOIN FETCH a.analyzerType WHERE a.id = :id";
        Query<Analyzer> query = entityManager.unwrap(Session.class).createQuery(hql, Analyzer.class);
        query.setParameter("id", Integer.valueOf(id));
        Analyzer result = query.uniqueResult();
        return Optional.ofNullable(result);
    }
}
