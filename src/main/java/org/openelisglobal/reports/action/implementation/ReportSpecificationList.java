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

package org.openelisglobal.reports.action.implementation;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.reports.form.ReportForm;

/** Represents a list for report specification */
public class ReportSpecificationList implements Serializable {

    private final String label;
    private final List<IdValuePair> list;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String selection;

    public ReportSpecificationList() {
        label = "";
        list = new ArrayList<>();
    }

    public ReportSpecificationList(List<IdValuePair> list, String label) {
        this.label = label;
        this.list = list;
    }

    public void setRequestParameters(ReportForm form) {
        form.setSelectList(this);
    }

    public String getLabel() {
        return label;
    }

    public List<IdValuePair> getList() {
        return list;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public String getSelectionAsName() {
        String selection = getSelection();

        for (IdValuePair pair : getList()) {
            if (selection.equals(pair.getId())) {
                return pair.getValue();
            }
        }

        return "";
    }
}
