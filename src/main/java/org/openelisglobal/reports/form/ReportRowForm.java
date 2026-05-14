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
package org.openelisglobal.reports.form;

import java.util.Map;
import org.openelisglobal.common.form.BaseForm;

/**
 * Generic report row data.
 *
 * <p>
 * This is a simple, logic-free DTO representing a single row of report data,
 * where column names map to their values.
 */
public class ReportRowForm extends BaseForm {

    private Map<String, Object> data;

    public ReportRowForm() {
        super();
    }

    public ReportRowForm(Map<String, Object> data) {
        this();
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
