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
package org.openelisglobal.common.services.registration;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.aggregatereporting.TestUsageUpdate;
import org.openelisglobal.dataexchange.malariareporting.MalariaReportingUpdate;
import org.openelisglobal.dataexchange.resultreporting.ResultReportingUpdate;

public class ResultUpdateRegister {
    public static List<IResultUpdate> getRegisteredUpdaters() {
        List<IResultUpdate> updaters = new ArrayList<IResultUpdate>();

        // kluge at this point, should be discoverable
        if (shouldReport(Property.reportResults)) {
            updaters.add(new ResultReportingUpdate());
        }

        if (shouldReport(Property.malariaCaseReport)) {
            updaters.add(new MalariaReportingUpdate());
        }

        if (shouldReport(Property.testUsageReporting)) {
            updaters.add(new TestUsageUpdate());
        }
        return updaters;
    }

    private static boolean shouldReport(Property property) {
        String reportResults = ConfigurationProperties.getInstance().getPropertyValueLowerCase(property);
        return ("true".equals(reportResults) || "enable".equals(reportResults));
    }
}
