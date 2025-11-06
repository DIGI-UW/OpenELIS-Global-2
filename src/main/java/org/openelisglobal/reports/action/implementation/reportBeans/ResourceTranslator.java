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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.gender.valueholder.Gender;
import org.openelisglobal.spring.util.SpringContext;

/**
 * RetroCI wants CSV export to list the numeric values that are prefixed on the
 * message resources. We use these classes to go from a DB ID (or other column
 * defined by getKey) to the resource and peel off the value at the front of the
 * localized resource string.
 *
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Feb 1, 2011
 */
public class ResourceTranslator<T extends BaseObject<PK>, PK extends Serializable> {
    /** placed on values that are not actually translated */
    public static final String NOT_FOUND_TAG = "%%";

    protected Map<PK, T> map = new HashMap<>();

    /** */
    public ResourceTranslator(List<T> ts) {
        for (T t : ts) {
            map.put(((BaseObject<PK>) t).getId(), t);
        }
    }

    /**
     * What part of the entity is used to lookup the record to find the localized
     * name?
     *
     * @param some object
     * @return some value of some field in that object, typicaly the ID.
     */
    protected String getKey(T t) {
        return ((BaseObject<PK>) t).getId().toString();
    }

    /** if it is not found in the translator, just return the original value. */
    public String translateOrNot(String original) {
        String translation = translate(original);
        if (translation.startsWith(NOT_FOUND_TAG)) {
            return original;
        } else {
            return translation;
        }
    }

    /**
     * This implements what CI was looking for. Get the localized string, and if it
     * starts with a number just show that.
     *
     * @param id
     * @return
     */
    public String translate(String id) {
        String resource = translateRaw(id);
        int dash = resource.indexOf('=');
        dash = (dash > 1) ? dash : resource.length();
        String userId = resource.substring(0, dash);
        return userId;
    }

    public String translateRaw(String id) {
        if ("0".equals(id)) {
            return "";
        }
        T t = map.get(id);
        if (t == null) {
            // return NOT_FOUND_TAG + " " + id + " not found in " +
            // this.getClass().getSimpleName() + " " + NOT_FOUND_TAG;
            LogEvent.logWarn(this.getClass().getSimpleName(), "translateRaw", NOT_FOUND_TAG + " " + id
                    + " not found in " + this.getClass().getSimpleName() + " " + NOT_FOUND_TAG);
            return id;
        }
        String resource = t.getLocalizedName();
        return resource;
    }

    public static class GenderTranslator extends ResourceTranslator<Gender, Integer> {
        private static GenderTranslator instance = null;

        public static GenderTranslator getInstance() {
            if (instance == null) {
                instance = new GenderTranslator();
            }
            return instance;
        }

        /**
         * Apparently at some locations there is a "0" gender meaning none. so here we
         * just translate 0/1 to M,F
         *
         * @see org.openelisglobal.reports.action.implementation.reportBeans.ResourceTranslator#translate(java.lang.String)
         */
        @Override
        public String translate(String id) {
            if ("0".equals(id)) {
                return "";
            }
            return super.translate(id);
        }

        private GenderTranslator() {
            super(SpringContext.getBean(GenderService.class).getAll());
        }

        @Override
        protected String getKey(Gender d) {
            return d.getGenderType();
        }
    }

    public static class DictionaryTranslator extends ResourceTranslator<Dictionary, String> {
        private static DictionaryTranslator instance = null;

        public static DictionaryTranslator getInstance() {
            if (instance == null) {
                instance = new DictionaryTranslator();
            }
            return instance;
        }

        public DictionaryTranslator() {
            super(SpringContext.getBean(DictionaryService.class).getAll());
        }

        @Override
        protected String getKey(Dictionary d) {
            return d.getId();
        }
    }
}
