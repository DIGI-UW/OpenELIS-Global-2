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
package org.openelisglobal.typeofsample.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;

public interface TypeOfSamplePanelDAO extends BaseDAO<TypeOfSamplePanel, String> {

    // public boolean insertData(TypeOfSamplePanel typeOfSamplePanel) throws
    // LIMSRuntimeException;

    // public void deleteData(String[] typeOfSamplePanelIds, String currentUserId)
    // throws
    // LIMSRuntimeException;

    List<TypeOfSamplePanel> getAllTypeOfSamplePanels() throws LIMSRuntimeException;

    List<TypeOfSamplePanel> getPageOfTypeOfSamplePanel(int startingRecNo) throws LIMSRuntimeException;

    void getData(TypeOfSamplePanel typeOfSamplePanel) throws LIMSRuntimeException;

    Integer getTotalTypeOfSamplePanelCount() throws LIMSRuntimeException;

    List<TypeOfSamplePanel> getTypeOfSamplePanelsForSampleType(String sampleType);

    List<TypeOfSamplePanel> getTypeOfSamplePanelsForPanel(String panelId) throws LIMSRuntimeException;
}
