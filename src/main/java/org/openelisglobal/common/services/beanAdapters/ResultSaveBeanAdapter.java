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

package org.openelisglobal.common.services.beanAdapters;

import java.math.BigDecimal;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.test.beanItems.TestResultItem;

public class ResultSaveBeanAdapter {

    public static ResultSaveBean fromTestResultItem(TestResultItem item) {
        ResultSaveBean bean = new ResultSaveBean();

        bean.setHasQualifiedResult(item.isHasQualifiedResult());
        bean.setResultType(item.getResultType());
        bean.setMultiSelectResultValues(item.getMultiSelectResultValues());
        bean.setTestId(item.getTestId());
        bean.setQualifiedResultId(item.getQualifiedResultId());
        bean.setQualifiedResultValue(item.getQualifiedResultValue());
        bean.setQualifiedDictionaryId(item.getQualifiedDictionaryId());
        bean.setResultId(item.getResultId());
        bean.setResultValue(item.getShadowResultValue());
        bean.setReportable(item.getReportable());
        bean.setLowerNormalRange(item.getLowerNormalRange());
        bean.setUpperNormalRange(item.getUpperNormalRange());
        bean.setSignificantDigits(item.getSignificantDigits());
        if (!GenericValidator.isBlankOrNull(item.getExpandedUncertainty())) {
            try {
                BigDecimal u = new BigDecimal(item.getExpandedUncertainty());
                if (u.compareTo(BigDecimal.ZERO) >= 0) {
                    bean.setExpandedUncertainty(u);
                    bean.setCoverageFactor(BigDecimal.valueOf(2.0));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return bean;
    }

    public static ResultSaveBean fromAnalysisItem(AnalysisItem item) {
        ResultSaveBean bean = new ResultSaveBean();

        bean.setHasQualifiedResult(item.isHasQualifiedResult());
        bean.setResultType(item.getResultType());
        bean.setMultiSelectResultValues(item.getMultiSelectResultValues());
        bean.setTestId(item.getTestId());
        bean.setQualifiedResultId(item.getQualifiedResultId());
        bean.setQualifiedResultValue(item.getQualifiedResultValue());
        bean.setQualifiedDictionaryId(item.getQualifiedDictionaryId());
        bean.setResultId(item.getResultId());
        bean.setResultValue(item.getResult());
        // bean.setReportable("N");
        bean.setSignificantDigits(item.getSignificantDigits());

        return bean;
    }
}
