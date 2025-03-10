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
package org.openelisglobal.login.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.Date;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

@Entity
@Table(name = "login_user")
public class LoginUser extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "login_user_generator")
    @SequenceGenerator(name = "login_user_generator", sequenceName = "login_user_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "login_name")
    @Size(max = 20)
    @NotBlank
    @ValidName(nameType = NameType.USERNAME, message = "username is invalid")
    private String loginName;

    @Column(name = "password")
    @Size(max = 255)
    private String password;

    @Transient
    private String newPassword;

    @Transient
    private String confirmPassword;

    @Column(name = "password_expired_dt")
    private Date passwordExpiredDT;

    @Column(name = "account_locked")
    @Size(max = 1)
    private String accountLocked;

    @Column(name = "account_disabled")
    @Size(max = 1)
    private String accountDisabled;

    @Column(name = "is_admin")
    @Size(max = 1)
    private String isAdmin;

    @Transient
    private int passwordExpiredDayNo;

    @Transient
    private int systemUserId;

    @Column(name = "user_time_out")
    @Size(max = 3)
    private String userTimeOut;

    public LoginUser() {
        super();
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setIsAdmin(String isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getIsAdmin() {
        return isAdmin;
    }

    public void setUserTimeOut(String userTimeOut) {
        this.userTimeOut = userTimeOut;
    }

    public String getUserTimeOut() {
        return userTimeOut;
    }

    public void setSystemUserId(int systemUserId) {
        this.systemUserId = systemUserId;
    }

    public int getSystemUserId() {
        return systemUserId;
    }

    public void setPasswordExpiredDayNo(int passwordExpiredDayNo) {
        this.passwordExpiredDayNo = passwordExpiredDayNo;
    }

    public int getPasswordExpiredDayNo() {
        return passwordExpiredDayNo;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Date getPasswordExpiredDate() {
        return passwordExpiredDT;
    }

    public void setPasswordExpiredDate(Date passwordExpiredDT) {
        this.passwordExpiredDT = passwordExpiredDT;
    }

    public void setPasswordExpiredDateForDisplay(String passwordExpiredDTForDisplay) {
        // also update the java.sql.Date
        String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);
        this.passwordExpiredDT = DateUtil.convertStringDateToSqlDate(passwordExpiredDTForDisplay, locale);
    }

    public String getPasswordExpiredDateForDisplay() {
        return DateUtil.convertSqlDateToStringDate(passwordExpiredDT);
    }

    public String getAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(String accountLocked) {
        this.accountLocked = accountLocked;
    }

    public String getAccountDisabled() {
        return accountDisabled;
    }

    public void setAccountDisabled(String accountDisabled) {
        this.accountDisabled = accountDisabled;
    }

    @Override
    public String getStringId() {
        return Integer.toString(id);
    }
}
