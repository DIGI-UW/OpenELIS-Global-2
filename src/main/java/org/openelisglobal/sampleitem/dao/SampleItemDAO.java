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
import java.util.Set;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface SampleItemDAO extends BaseDAO<SampleItem, String> {

    public boolean insertData(SampleItem sampleItem) throws LIMSRuntimeException;

    // public void deleteData(List<SampleItem> sampleItems) throws
    // LIMSRuntimeException;

    public List<SampleItem> getAllSampleItems() throws LIMSRuntimeException;

    public List<SampleItem> getPageOfSampleItems(int startingRecNo) throws LIMSRuntimeException;

    public void getData(SampleItem sampleItem) throws LIMSRuntimeException;

    // public void updateData(SampleItem sampleItem) throws LIMSRuntimeException;

    public void getDataBySample(SampleItem sampleItem) throws LIMSRuntimeException;

    public List<SampleItem> getSampleItemsBySampleId(String id) throws LIMSRuntimeException;

    public List<SampleItem> getSampleItemsBySampleIdAndType(String sampleId, TypeOfSample typeOfSample)
            throws LIMSRuntimeException;

    public List<SampleItem> getSampleItemsBySampleIdAndStatus(String id, Set<Integer> includedStatusList)
            throws LIMSRuntimeException;

    public SampleItem getData(String sampleItemId) throws LIMSRuntimeException;

    public List<SampleItem> getSampleItemsByExternalID(String externalId) throws LIMSRuntimeException;

    public boolean insertAliquots(SampleItem lastSampleItem, List<SampleItem> sampleItemsToInsert,
            List<List<String>> analysisGroups) throws LIMSRuntimeException;

}
