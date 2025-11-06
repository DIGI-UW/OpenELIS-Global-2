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
import org.openelisglobal.common.formfields.FormFields.Field;

public class HT_ClinicalFormFields implements IFormFieldsForImplementation {

    @Override
    public HashMap<Field, FormField> getImplementationAttributes() {
        HashMap<Field, FormField> settings = new HashMap<>();
        settings.put(Field.InlineOrganizationTypes, new FormField(Boolean.TRUE));
        settings.put(Field.DepersonalizedResults, new FormField(Boolean.TRUE));
        settings.put(Field.OrgLocalAbrev, new FormField(Boolean.FALSE));
        settings.put(Field.OrganizationMultiUnit, new FormField(Boolean.FALSE));
        settings.put(Field.OrganizationOrgId, new FormField(Boolean.FALSE));
        settings.put(Field.ADDRESS_CITY, new FormField(Boolean.FALSE));
        settings.put(Field.ADDRESS_DEPARTMENT, new FormField(Boolean.TRUE));
        settings.put(Field.PatientRequired, new FormField(Boolean.TRUE));
        settings.put(Field.SampleCondition, new FormField(Boolean.TRUE));
        settings.put(Field.NON_CONFORMITY_SITE_LIST, new FormField(Boolean.TRUE));
        settings.put(Field.ValueHozSpaceOnResults, new FormField(Boolean.TRUE));
        settings.put(Field.SampleEntryUseReceptionHour, new FormField(Boolean.TRUE));
        settings.put(Field.CollectionDate, new FormField(Boolean.TRUE));
        settings.put(Field.CollectionTime, new FormField(Boolean.TRUE));
        settings.put(Field.SAMPLE_ENTRY_USE_REFFERING_PATIENT_NUMBER, new FormField(Boolean.TRUE));
        settings.put(Field.SampleEntryRequestingSiteSampleId, new FormField(Boolean.FALSE));
        return settings;
    }
}
