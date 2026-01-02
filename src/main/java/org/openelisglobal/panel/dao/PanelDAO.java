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
package org.openelisglobal.panel.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.panel.valueholder.Panel;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface PanelDAO extends BaseDAO<Panel, String> {

    // public boolean insertData(Panel panel) throws LIMSRuntimeException;

    // public void deleteData(List panels) throws LIMSRuntimeException;

    List<Panel> getAllActivePanels() throws LIMSRuntimeException;

    List<Panel> getPageOfPanels(int startingRecNo) throws LIMSRuntimeException;

    void getData(Panel panel) throws LIMSRuntimeException;

    // public void updateData(Panel panel) throws LIMSRuntimeException;

    List<Panel> getActivePanels(String filter) throws LIMSRuntimeException;

    List<Panel> getAllPanels() throws LIMSRuntimeException;

    Panel getPanelByName(Panel panel) throws LIMSRuntimeException;

    Integer getTotalPanelCount() throws LIMSRuntimeException;

    String getNameForPanelId(String panelId);

    String getDescriptionForPanelId(String id);

    String getIdForPanelName(String name);

    Panel getPanelByName(String panelName) throws LIMSRuntimeException;

    Panel getPanelById(String id) throws LIMSRuntimeException;

    boolean duplicatePanelExists(Panel panel);

    boolean duplicatePanelDescriptionExists(Panel panel);

    boolean duplicatePanelCodeExists(Panel panel);

    void clearIDMaps();

    /**
     * Ensure the database sequence used for panel IDs is advanced to avoid
     * collisions with preloaded dataset IDs (tests/liquibase may insert explicit
     * IDs).
     */
    void ensureSequence();

    Panel getPanelByLoincCode(String loincCode);

    /**
     * Update panel fields without relying on optimistic locking (used when version
     * is null or legacy rows exist). Implementations should update key fields and
     * the lastupdated timestamp using a bulk JPQL update to avoid version-based
     * WHERE checks failing on NULL values.
     */
    void updatePanelFields(Panel panel);
}
