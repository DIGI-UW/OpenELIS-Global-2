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
 */
package org.openelisglobal.renamemethod.valueholder;

import java.sql.Date;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.localization.valueholder.Localization;

public class RenameMethod extends EnumValueItemImpl {

    private String id;

    private String methodName;

    private String description;

    private String reportingDescription;

    private Date activeBeginDate = null;

    private String activeBeginDateForDisplay = null;

    private Date activeEndDate = null;

    private String activeEndDateForDisplay = null;

    private String isActive;

    private ValueHolderInterface localization;

    public RenameMethod() {
        super();
        localization = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setReportingDescription(String reportingDescription) {
        this.reportingDescription = reportingDescription;
    }

    public String getReportingDescription() {
        return reportingDescription;
    }

    public void setActiveBeginDate(Date activeBeginDate) {
        this.activeBeginDate = activeBeginDate;
        this.activeBeginDateForDisplay = DateUtil.convertSqlDateToStringDate(activeBeginDate);
    }

    public Date getActiveBeginDate() {
        return activeBeginDate;
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
        this.activeEndDateForDisplay = DateUtil.convertSqlDateToStringDate(activeEndDate);
    }

    public Date getActiveEndDate() {
        return activeEndDate;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setActiveBeginDateForDisplay(String activeBeginDateForDisplay) {
        this.activeBeginDateForDisplay = activeBeginDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeBeginDate = DateUtil.convertStringDateToSqlDate(this.activeBeginDateForDisplay, locale);
    }

    public String getActiveBeginDateForDisplay() {
        return activeBeginDateForDisplay;
    }

    public void setActiveEndDateForDisplay(String activeEndDateForDisplay) {
        this.activeEndDateForDisplay = activeEndDateForDisplay;
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.activeEndDate = DateUtil.convertStringDateToSqlDate(activeEndDateForDisplay, locale);
    }

    public String getActiveEndDateForDisplay() {
        return activeEndDateForDisplay;
    }

    public Localization getLocalization() {
        return (Localization) localization.getValue();
    }

    public void setLocalization(Localization localization) {
        this.localization.setValue(localization);
    }
}
