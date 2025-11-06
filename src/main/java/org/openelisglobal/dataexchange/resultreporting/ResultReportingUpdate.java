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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.resultreporting;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.valueholder.Result;

public class ResultReportingUpdate implements IResultUpdate {

    @Override
    public void transactionalUpdate(IResultSaveService resultService) throws LIMSRuntimeException {
        // no-op
    }

    @Override
    public void postTransactionalCommitUpdate(IResultSaveService resultService) {
        ResultReportingCollator collator = new ResultReportingCollator();
        List<Result> updatedResults = new ArrayList<Result>();

        for (ResultSet resultSet : resultService.getNewResults()) {
            boolean success = collator.addResult(resultSet.result, resultSet.patient, false, false);
            if (success) {
                updatedResults.add(resultSet.result);
            }
        }
        for (ResultSet resultSet : resultService.getModifiedResults()) {
            boolean success = collator.addResult(resultSet.result, resultSet.patient, true, false);
            if (success) {
                updatedResults.add(resultSet.result);
            }
        }

        ResultReportingTransfer transfer = new ResultReportingTransfer();
        transfer.sendResults(collator.getResultReport(), updatedResults,
                ConfigurationProperties.getInstance().getPropertyValue(Property.resultReportingURL));
    }
}
