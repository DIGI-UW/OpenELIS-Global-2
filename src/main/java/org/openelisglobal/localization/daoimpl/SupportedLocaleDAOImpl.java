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

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.localization.dao.SupportedLocaleDAO;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SupportedLocaleDAOImpl extends BaseDAOImpl<SupportedLocale, String>
        implements SupportedLocaleDAO {

    public SupportedLocaleDAOImpl() {
        super(SupportedLocale.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportedLocale> getAllActive() {
        return getAllMatchingOrdered("active", true, "sortOrder", false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportedLocale> getFallback() {
        List<SupportedLocale> results = getAllMatching("fallback", true);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportedLocale> getByLocaleCode(String localeCode) {
        List<SupportedLocale> results = getAllMatching("localeCode", localeCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
