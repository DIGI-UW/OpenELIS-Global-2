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

package org.openelisglobal.localization.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.localization.dao.SupportedLocaleDAO;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportedLocaleServiceImpl extends BaseObjectServiceImpl<SupportedLocale, String>
        implements SupportedLocaleService {

    private static final String DEFAULT_FALLBACK_LOCALE = "en";

    @Autowired
    private SupportedLocaleDAO supportedLocaleDAO;

    public SupportedLocaleServiceImpl() {
        super(SupportedLocale.class);
    }

    @Override
    protected SupportedLocaleDAO getBaseObjectDAO() {
        return supportedLocaleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportedLocale> getAllActive() {
        return supportedLocaleDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Locale> getAllActiveAsLocales() {
        return getAllActive().stream().map(SupportedLocale::toLocale).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportedLocale> getFallback() {
        return supportedLocaleDAO.getFallback();
    }

    @Override
    @Transactional(readOnly = true)
    public String getFallbackLocaleCode() {
        return getFallback().map(SupportedLocale::getLocaleCode).orElse(DEFAULT_FALLBACK_LOCALE);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportedLocale> getByLocaleCode(String localeCode) {
        return supportedLocaleDAO.getByLocaleCode(localeCode);
    }
}
