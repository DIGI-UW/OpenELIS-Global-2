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
package org.openelisglobal.panelitem.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.panelitem.valueholder.PanelItem;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface PanelItemDAO extends BaseDAO<PanelItem, String> {

    // public boolean insertData(PanelItem panelItem) throws LIMSRuntimeException;

    // public void deleteData(List panelItems) throws LIMSRuntimeException;

    List<PanelItem> getAllPanelItems() throws LIMSRuntimeException;

    List<PanelItem> getPageOfPanelItems(int startingRecNo) throws LIMSRuntimeException;

    void getData(PanelItem panelItem) throws LIMSRuntimeException;

    // public void updateData(PanelItem panelItem) throws LIMSRuntimeException;

    List<PanelItem> getPanelItems(String filter) throws LIMSRuntimeException;

    Integer getTotalPanelItemCount() throws LIMSRuntimeException;

    boolean getDuplicateSortOrderForPanel(PanelItem panelItem);

    // public List getPanelItemByPanel(Panel panel, boolean onlyTestsFullySetup)
    // throws
    // LIMSRuntimeException;

    List<PanelItem> getPanelItemsForPanel(String panelId) throws LIMSRuntimeException;

    List<PanelItem> getPanelItemByTestId(String id) throws LIMSRuntimeException;

    List<PanelItem> getPanelItemsForPanelAndItemList(String panelId, List<Integer> testList)
            throws LIMSRuntimeException;
}
