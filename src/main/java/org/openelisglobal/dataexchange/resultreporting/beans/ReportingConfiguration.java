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
package org.openelisglobal.dataexchange.resultreporting.beans;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import org.hibernate.validator.constraints.URL;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.resultreporting.form.ResultReportingConfigurationForm;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class ReportingConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^$|^enable$|^disable$", groups = { ResultReportingConfigurationForm.ResultReportConfig.class })
    private String enabled;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultReportingConfigurationForm.ResultReportConfig.class })
    private String enabledId;

    @URL(groups = { ResultReportingConfigurationForm.ResultReportConfig.class })
    private String url;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultReportingConfigurationForm.ResultReportConfig.class })
    private String urlId;

    private boolean showAuthentication = false;

    @ValidName(nameType = NameType.USERNAME, message = "username is invalid", groups = {
            ResultReportingConfigurationForm.ResultReportConfig.class })
    private String userName;

    private String userNameId;

    private String password;

    private String passwordId;

    private boolean showBacklog = false;

    @Pattern(regexp = "^[0-9]$*")
    private String backlogSize = "0";

    private boolean isScheduled = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultReportingConfigurationForm.ResultReportConfig.class })
    private String schedulerId;

    @Pattern(regexp = ValidationHelper.HOUR_REGEX, groups = {
            ResultReportingConfigurationForm.ResultReportConfig.class })
    private String scheduleHours;

    @Pattern(regexp = ValidationHelper.MINUTES_REGEX, groups = {
            ResultReportingConfigurationForm.ResultReportConfig.class })
    private String scheduleMin;

    private String title;

    private String connectionTestIdentifier;

    public String getEnabledId() {
        return enabledId;
    }

    public void setEnabledId(String enabledId) {
        this.enabledId = enabledId;
    }

    public String getUrlId() {
        return urlId;
    }

    public void setUrlId(String urlId) {
        this.urlId = urlId;
    }

    public String getUserNameId() {
        return userNameId;
    }

    public void setUserNameId(String userNameId) {
        this.userNameId = userNameId;
    }

    public String getPasswordId() {
        return passwordId;
    }

    public void setPasswordId(String passwordId) {
        this.passwordId = passwordId;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBacklogSize() {
        return backlogSize;
    }

    public void setBacklogSize(String backlogSize) {
        this.backlogSize = backlogSize;
    }

    public String getScheduleHours() {
        return scheduleHours;
    }

    public void setScheduleHours(String scheduleHours) {
        this.scheduleHours = scheduleHours;
    }

    public String getScheduleMin() {
        return scheduleMin;
    }

    public void setScheduleMin(String scheduleMin) {
        this.scheduleMin = scheduleMin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getIsScheduled() {
        return isScheduled;
    }

    public void setIsScheduled(boolean isScheduled) {
        this.isScheduled = isScheduled;
    }

    public String getSchedulerId() {
        return schedulerId;
    }

    public void setSchedulerId(String schedulerId) {
        this.schedulerId = schedulerId;
    }

    public boolean getShowBacklog() {
        return showBacklog;
    }

    public void setShowBacklog(boolean showBacklog) {
        this.showBacklog = showBacklog;
    }

    public boolean getShowAuthentication() {
        return showAuthentication;
    }

    public void setShowAuthentication(boolean showAuthentication) {
        this.showAuthentication = showAuthentication;
    }

    public String getConnectionTestIdentifier() {
        return connectionTestIdentifier;
    }

    public void setConnectionTestIdentifier(String connectionTestIdentifier) {
        this.connectionTestIdentifier = connectionTestIdentifier;
    }
}
