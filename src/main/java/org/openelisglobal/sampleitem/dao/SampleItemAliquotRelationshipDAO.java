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
package org.openelisglobal.sampleitem.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleitem.valueholder.SampleItemAliquotRelationship;

/**
 * Data Access Object for SampleItemAliquotRelationship entity.
 *
 * <p>
 * Provides methods to query aliquot relationships and retrieve metadata about
 * aliquoting operations. Extends BaseDAO for standard CRUD operations.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleItemAliquotRelationship
 */
public interface SampleItemAliquotRelationshipDAO extends BaseDAO<SampleItemAliquotRelationship, Long> {

    /**
     * Get the maximum sequence number for aliquots of a given parent sample item.
     * Used to determine the next sequence number when creating a new aliquot.
     *
     * @param parentSampleItem the parent sample item
     * @return the maximum sequence number, or 0 if no aliquots exist
     * @throws LIMSRuntimeException if database error occurs
     */
    Integer getMaxSequenceNumber(SampleItem parentSampleItem) throws LIMSRuntimeException;

    /**
     * Get all aliquot relationships for a given parent sample item. Returns
     * relationships ordered by sequence number ascending.
     *
     * @param parentSampleItem the parent sample item
     * @return list of aliquot relationships for this parent
     * @throws LIMSRuntimeException if database error occurs
     */
    List<SampleItemAliquotRelationship> getByParentSampleItem(SampleItem parentSampleItem) throws LIMSRuntimeException;

    /**
     * Get the aliquot relationship for a specific parent and child sample item
     * pair.
     *
     * @param parentSampleItem the parent sample item
     * @param childSampleItem  the child sample item (aliquot)
     * @return the aliquot relationship, or null if not found
     * @throws LIMSRuntimeException if database error occurs
     */
    SampleItemAliquotRelationship getByParentAndChild(SampleItem parentSampleItem, SampleItem childSampleItem)
            throws LIMSRuntimeException;
}
