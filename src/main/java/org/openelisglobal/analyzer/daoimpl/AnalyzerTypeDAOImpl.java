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
package org.openelisglobal.analyzer.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerTypeDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerTypeDAOImpl extends BaseDAOImpl<AnalyzerType, String> implements AnalyzerTypeDAO {

    public AnalyzerTypeDAOImpl() {
        super(AnalyzerType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerType> findByIdWithInstances(String id) {
        String hql = "SELECT t FROM AnalyzerType t LEFT JOIN FETCH t.instances WHERE t.id = :id";
        TypedQuery<AnalyzerType> query = entityManager.createQuery(hql, AnalyzerType.class);
        query.setParameter("id", id);
        return query.getResultList().stream().findFirst();
    }
}
