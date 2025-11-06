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

public class CI_RETROFormFields implements IFormFieldsForImplementation {

    @Override
    public HashMap<FormFields.Field, FormField> getImplementationAttributes() {
        HashMap<FormFields.Field, FormField> settings = new HashMap<>();

        settings.put(Field.StNumber, new FormField(Boolean.FALSE));
        settings.put(Field.AKA, new FormField(Boolean.FALSE));
        settings.put(Field.MothersName, new FormField(Boolean.FALSE));
        settings.put(Field.PatientType, new FormField(Boolean.TRUE));
        settings.put(Field.InsuranceNumber, new FormField(Boolean.FALSE));
        settings.put(Field.CollectionDate, new FormField(Boolean.TRUE));
        settings.put(Field.OrgLocalAbrev, new FormField(Boolean.FALSE));
        settings.put(Field.InlineOrganizationTypes, new FormField(Boolean.TRUE));
        settings.put(Field.ProviderInfo, new FormField(Boolean.FALSE));
        settings.put(Field.NationalID, new FormField(Boolean.TRUE));
        settings.put(Field.SearchSampleStatus, new FormField(Boolean.TRUE));
        settings.put(Field.OrgLocalAbrev, new FormField(Boolean.FALSE));
        settings.put(Field.OrganizationAddressInfo, new FormField(Boolean.FALSE));
        settings.put(Field.DepersonalizedResults, new FormField(Boolean.TRUE));
        settings.put(Field.SEARCH_PATIENT_WITH_LAB_NO, new FormField(Boolean.TRUE));
        settings.put(Field.Project, new FormField(Boolean.TRUE));
        settings.put(Field.QA_FULL_PROVIDER_INFO, new FormField(Boolean.FALSE));
        settings.put(Field.QASubjectNumber, new FormField(Boolean.FALSE));
        settings.put(Field.ADDRESS_COMMUNE, new FormField(Boolean.FALSE));
        settings.put(Field.ADDRESS_VILLAGE, new FormField(Boolean.FALSE));

        return settings;
    }
}
