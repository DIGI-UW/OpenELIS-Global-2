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
package org.openelisglobal.sampleitem.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sampleitem.dao.SampleItemAliquotRelationshipDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleitem.valueholder.SampleItemAliquotRelationship;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SampleItemAliquotRelationshipDAO.
 *
 * <p>
 * Provides database access methods for querying aliquot relationships and
 * metadata. Uses HQL queries for custom retrieval operations beyond standard
 * CRUD.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleItemAliquotRelationship
 */
@Component
@Transactional
public class SampleItemAliquotRelationshipDAOImpl extends BaseDAOImpl<SampleItemAliquotRelationship, Long>
        implements SampleItemAliquotRelationshipDAO {

    public SampleItemAliquotRelationshipDAOImpl() {
        super(SampleItemAliquotRelationship.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMaxSequenceNumber(SampleItem parentSampleItem) throws LIMSRuntimeException {
        try {
            String hql = "SELECT COALESCE(MAX(r.sequenceNumber), 0) FROM SampleItemAliquotRelationship r"
                    + " WHERE r.parentSampleItem.id = :parentId";
            Query<Integer> query = entityManager.unwrap(Session.class).createQuery(hql, Integer.class);
            query.setParameter("parentId", parentSampleItem.getId());
            Integer maxSeq = query.uniqueResult();
            return maxSeq != null ? maxSeq : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleItemAliquotRelationshipDAO getMaxSequenceNumber()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItemAliquotRelationship> getByParentSampleItem(SampleItem parentSampleItem)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM SampleItemAliquotRelationship r" + " WHERE r.parentSampleItem.id = :parentId"
                    + " ORDER BY r.sequenceNumber ASC";
            Query<SampleItemAliquotRelationship> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleItemAliquotRelationship.class);
            query.setParameter("parentId", parentSampleItem.getId());
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleItemAliquotRelationshipDAO getByParentSampleItem()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleItemAliquotRelationship getByParentAndChild(SampleItem parentSampleItem, SampleItem childSampleItem)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM SampleItemAliquotRelationship r" + " WHERE r.parentSampleItem.id = :parentId"
                    + " AND r.childSampleItem.id = :childId";
            Query<SampleItemAliquotRelationship> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleItemAliquotRelationship.class);
            query.setParameter("parentId", parentSampleItem.getId());
            query.setParameter("childId", childSampleItem.getId());
            List<SampleItemAliquotRelationship> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SampleItemAliquotRelationshipDAO getByParentAndChild()", e);
        }
    }
}
