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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.localization.dao.LocalizationValueDAO;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class LocalizationValueDAOImpl extends BaseDAOImpl<LocalizationValue, String> implements LocalizationValueDAO {

    public LocalizationValueDAOImpl() {
        super(LocalizationValue.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalizationValue> getByLocalizationId(String localizationId) {
        return getAllMatching("localization.id", localizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalizationValue> getByLocalizationIdAndLocale(String localizationId, String locale) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("localization.id", localizationId);
        propertyValues.put("locale", locale);
        List<LocalizationValue> results = getAllMatching(propertyValues);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalizationValue> getByLocale(String locale) {
        return getAllMatching("locale", locale);
    }
}
