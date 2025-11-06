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
import java.util.Map.Entry;
import org.openelisglobal.common.formfields.FormFields.Field;

public abstract class AFormFields {

    protected abstract HashMap<FormFields.Field, FormField> getSetAttributes();

    protected abstract HashMap<FormFields.Field, FormField> getDefaultAttributes();

    public Map<Field, FormField> getFieldFormSet() throws IllegalStateException {

        Map<FormFields.Field, FormField> defaultAttributes = getDefaultAttributes();
        Map<FormFields.Field, FormField> setAttributes = getSetAttributes();
        // if set attribute has a null field value, replace it with the default
        for (Entry<Field, FormField> setFieldEntry : setAttributes.entrySet()) {
            if (setAttributes.get(setFieldEntry.getKey()).getLabelKey() == null) {
                setAttributes.get(setFieldEntry.getKey())
                        .setLabelKey(defaultAttributes.get(setFieldEntry.getKey()).getLabelKey());
            }
            if (setAttributes.get(setFieldEntry.getKey()).getInUse() == null) {
                setAttributes.get(setFieldEntry.getKey())
                        .setInUse(defaultAttributes.get(setFieldEntry.getKey()).getInUse());
            }
        }

        defaultAttributes.putAll(setAttributes);
        return defaultAttributes;
    }
}
