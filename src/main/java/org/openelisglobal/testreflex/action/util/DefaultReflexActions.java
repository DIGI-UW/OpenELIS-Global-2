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
package org.openelisglobal.testreflex.action.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.spring.util.SpringContext;

public class DefaultReflexActions extends ReflexAction {

    private static final String HIV_N_SCRIPT = "HIV N";
    private static final String HIV_INDETERMINATE_SCRIPT = "HIV Indeterminate";
    private static final String HIV_POSITIVE_SCRIPT = "HIV Positive";

    private static Analyte ANALYTE_CONCLUSION;
    private static Map<String, String> hivStatusToDictionaryIDMap;

    static {
        hivStatusToDictionaryIDMap = new HashMap<>();

        DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

        List<Dictionary> dictionaryList = dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort("HIVResult");

        for (Dictionary dictionary : dictionaryList) {
            if (dictionary.getDictEntry().equals("Positive")) {
                hivStatusToDictionaryIDMap.put(HIV_POSITIVE_SCRIPT, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("Negative")) {
                hivStatusToDictionaryIDMap.put(HIV_N_SCRIPT, dictionary.getId());
            } else if (dictionary.getDictEntry().equals("Indeterminate")) {
                hivStatusToDictionaryIDMap.put(HIV_INDETERMINATE_SCRIPT, dictionary.getId());
            }
        }

        AnalyteService analyteService = SpringContext.getBean(AnalyteService.class);
        Analyte analyte = new Analyte();
        analyte.setAnalyteName("Conclusion");
        ANALYTE_CONCLUSION = analyteService.getAnalyteByName(analyte, false);
    }

    @Override
    protected void handleScriptletAction(Scriptlet scriptlet) {
        if (scriptlet != null && INTERPERET_TYPE.equals(scriptlet.getCodeType())) {
            String action = scriptlet.getCodeSource();

            if (GenericValidator.isBlankOrNull(action)) {
                return;
            }

            if (action.equals(HIV_INDETERMINATE_SCRIPT) || action.equals(HIV_N_SCRIPT)
                    || action.equals(HIV_POSITIVE_SCRIPT)) {
                addHIVConclusion(action);
            }
        }
    }

    private void addHIVConclusion(String action) {

        finalResult = new Result();
        finalResult.setValue(hivStatusToDictionaryIDMap.get(action));
        finalResult.setResultType("D");
        finalResult.setIsReportable("T");
        finalResult.setAnalyte(ANALYTE_CONCLUSION);
    }
}
