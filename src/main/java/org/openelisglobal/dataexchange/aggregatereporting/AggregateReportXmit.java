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
package org.openelisglobal.dataexchange.aggregatereporting;

import java.sql.Timestamp;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;

public class AggregateReportXmit {

    private Timestamp eventDate;
    private String data;

    public AggregateReportXmit() {
    } // Makes CASTOR happy

    public AggregateReportXmit(ReportExternalExport report) {
        eventDate = report.getEventDate();
        data = report.getData().replace("\n", "");
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
