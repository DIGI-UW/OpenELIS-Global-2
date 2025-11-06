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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.reports.form.ReportForm;

/**
 * A class for generating reports that have counts of various things, organized
 * into groups. The group is the high-level idea in this class. If you don't
 * want any groupings, (1) use a simplier report format and (2) put "" (or null)
 * for all group values. The reason this class is so generic is that it looked
 * like there was going to be more than one table of counts.
 */
public abstract class NonConformityBy extends Report implements IReportCreator {

    protected List<CountReportItem> reportItems = new ArrayList<>();

    protected DateRange dateRange;

    public static class CountReportItem {
        private String group;
        private String category;
        private int categoryCount;
        private int groupCount;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Integer getCategoryCount() {
            return categoryCount;
        }

        public void setCategoryCount(Integer count) {
            this.categoryCount = count;
        }

        public void setGroupCount(Integer groupCount) {
            this.groupCount = groupCount;
        }

        public Integer getGroupCount() {
            return groupCount;
        }

        public void addCategoryCount(Integer inc) {
            categoryCount += inc;
        }

        public void addGroupCount(Integer inc) {
            groupCount += inc;
        }
    }

    public static final class CountReportItemByGroupCategoryCompartor implements Comparator<CountReportItem> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(CountReportItem left, CountReportItem right) {
            int result;
            result = StringUtil.compareWithNulls(left.getGroup(), right.getGroup());
            if (result != 0) {
                return result;
            }
            return StringUtil.compareWithNulls(left.getCategory(), right.getCategory());
        }
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        dateRange = new DateRange(form.getLowerDateRange(), form.getUpperDateRange());

        createReportParameters();
        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }
        createReportItems();
        if (this.reportItems.size() == 0) {
            add1LineErrorMessage("report.error.message.noPrintableItems");
        }
        return;
    }

    protected void sortReportItems() {
        Collections.sort(reportItems, new CountReportItemByGroupCategoryCompartor());
    }

    /** */
    abstract void createReportItems();

    /** check everything */
    private boolean validateSubmitParameters() {
        return (dateRange.validateHighLowDate("report.error.message.date.received.missing"));
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.Report#getReportDataSource()
     */
    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }
}
