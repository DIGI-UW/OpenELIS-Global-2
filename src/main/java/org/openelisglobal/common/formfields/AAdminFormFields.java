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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.formfields;

import java.util.HashMap;
import java.util.Map;

public abstract class AAdminFormFields {

    protected abstract HashMap<AdminFormFields.Field, Boolean> getSetAttributes();

    protected abstract HashMap<AdminFormFields.Field, Boolean> getDefaultAttributes();

    public Map<AdminFormFields.Field, Boolean> getFieldFormSet() throws IllegalStateException {

        Map<AdminFormFields.Field, Boolean> defaultAttributes = getDefaultAttributes();
        Map<AdminFormFields.Field, Boolean> setAttributes = getSetAttributes();

        if (defaultAttributes == null) {
            defaultAttributes = new HashMap<AdminFormFields.Field, Boolean>();
        }

        if (setAttributes == null) {
            setAttributes = new HashMap<AdminFormFields.Field, Boolean>();
        }

        defaultAttributes.putAll(setAttributes);

        return defaultAttributes;
    }
}
