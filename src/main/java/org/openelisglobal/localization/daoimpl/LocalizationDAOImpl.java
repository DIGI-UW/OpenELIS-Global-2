/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.localization.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.localization.dao.LocalizationDAO;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** */
@Component
@Transactional
public class LocalizationDAOImpl extends BaseDAOImpl<Localization, String> implements LocalizationDAO {

    public LocalizationDAOImpl() {
        super(Localization.class);
    }

    // @Override
    // public Localization getLocalizationById(String id) throws
    // LIMSRuntimeException {
    // try {
    // Localization localization =
    // entityManager.unwrap(Session.class).get(Localization.class, id);
    // // closeSession(); // CSL remove old
    // return localization;
    // } catch (HibernateException e) {
    // handleException(e, "getLocalizationById");
    // }
    // return null;
    // }

    // @Override
    // public void updateData(Localization localization) throws LIMSRuntimeException
    // {
    // Localization oldData = readLocalization(localization.getId());
    //
    // try {
    //
    // auditDAO.saveHistory(localization, oldData, localization.getSysUserId(),
    // IActionConstants.AUDIT_TRAIL_UPDATE, "LOCALIZATION");
    // } catch (RuntimeException e) {
    // handleException(e, "Error in Localization AuditTrail updateData()");
    // }
    //
    // try {
    // entityManager.unwrap(Session.class).merge(localization);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove old(localization);
    // // entityManager.unwrap(Session.class).refresh // CSL remove
    // old(localization);
    // } catch (HibernateException e) {
    // handleException(e, "updateData");
    // }
    // }

    // public Localization readLocalization(String idString) {
    // try {
    // Localization localization =
    // entityManager.unwrap(Session.class).get(Localization.class,
    // idString);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // return localization;
    // } catch (RuntimeException e) {
    // handleException(e, "readLocalization");
    // }
    //
    // return null;
    // }

}
