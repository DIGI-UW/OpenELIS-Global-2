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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;

import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

public class EIDTypeOfClinicStrategy implements ICSVColumnCustomStrategy {

    private String CLINIC_OTHER_OBS_HISTORY_TYPE_ID;
    private String CLINIC_OTHER_DICT_ID;

    public EIDTypeOfClinicStrategy() {
        Dictionary clinicOtherDictionary = SpringContext.getBean(DictionaryService.class)
                .getDictionaryEntrysByNameAndCategoryDescription("4 = Other(Specify)", "EID Type of Clinic");

        if (clinicOtherDictionary != null) {
            CLINIC_OTHER_DICT_ID = clinicOtherDictionary.getId();
        }

        ObservationHistoryType observationType = SpringContext.getBean(ObservationHistoryTypeService.class)
                .getByName("eidTypeOfClinicOther");

        if (observationType != null) {
            CLINIC_OTHER_OBS_HISTORY_TYPE_ID = observationType.getId();
        }
    }

    @Override
    public String translate(String value, String accessionNumber, String csvName, String dbName) {
        if (CLINIC_OTHER_DICT_ID.equals(value)) { // clinic "other" was entered
            Sample sample = SpringContext.getBean(SampleService.class).getSampleByAccessionNumber(accessionNumber);

            if (sample != null) {
                ObservationHistory observation = SpringContext.getBean(ObservationHistoryService.class)
                        .getObservationHistoriesBySampleIdAndType(sample.getId(), CLINIC_OTHER_OBS_HISTORY_TYPE_ID);

                if (observation != null) {
                    return observation.getValue();
                }
            }
        }
        return isBlankOrNull(value) ? "" : ResourceTranslator.DictionaryTranslator.getInstance().translate(value);
    }
}
