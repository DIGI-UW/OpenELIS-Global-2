package org.openelisglobal.systemuser.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.role.action.bean.DisplayRole;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class UnifiedSystemUserForm extends BaseForm {
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String loginUserId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String systemUserId = "";

    @NotBlank
    @ValidName(nameType = NameType.USERNAME, message = "username is invalid")
    private String userLoginName = "";

    // in validator
    private String userPassword;

    // in validator
    private String confirmPassword;

    @NotBlank
    @ValidName(nameType = NameType.FIRST_NAME)
    private String userFirstName = "";

    @NotBlank
    @ValidName(nameType = NameType.LAST_NAME)
    private String userLastName = "";

    // for display
    private List<DisplayRole> globalRoles;

    // for display
    private List<DisplayRole> labUnitRoles;

    // for display
    private List<IdValuePair> testSections;

    /**
     * There are multiple fields in the ui-form mapped to this field, because the
     * ui-form can dynamically create more fields mapped to this same field(path),
     * this field is only used to get values from the form as a single String with
     * comma ,separated values ie "value1 ,value2"
     */
    private String testSectionId;

    /**
     * There are multiple fields in the ui-form mapped to this field, because the
     * ui-form can dynamically create more fields mapped to this same field(path),
     * this field is only used to get values from the form as a List of Strings
     */
    private List<String> selectedLabUnitRoles;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedRoles;

    @NotBlank
    @ValidDate(relative = DateRelation.FUTURE)
    private String expirationDate;

    @NotBlank
    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String accountLocked = "N";

    @NotBlank
    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String accountDisabled = "N";

    @NotBlank
    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String accountActive = "Y";

    @NotBlank
    @Pattern(regexp = "^[0-9]*$")
    private String timeout;

    private Timestamp systemUserLastupdated;

    /**
     * This field passes the user Lab Unit Roles data to the ui-form in form of a
     * json object , in order to dynamically render sets of Lab Unit Roles with data
     * ,with fields that are mapped to the same path ie testSectionId and
     * selectedLabUnitRoles
     **/
    @JsonIgnore
    private JSONObject userLabRoleData;

    // for display
    private JSONArray systemUsers;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String systemUserIdToCopy = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.YES_NO_REGEX)
    private String allowCopyUserRoles = "N";

    public UnifiedSystemUserForm() {
        setFormName("unifiedSystemUserForm");
    }

    public String getLoginUserId() {
        return loginUserId;
    }

    public void setLoginUserId(String loginUserId) {
        this.loginUserId = loginUserId;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getUserLoginName() {
        return userLoginName;
    }

    public void setUserLoginName(String userLoginName) {
        this.userLoginName = userLoginName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public List<DisplayRole> getGlobalRoles() {
        return globalRoles;
    }

    public void setGlobalRoles(List<DisplayRole> globalRoles) {
        this.globalRoles = globalRoles;
    }

    public List<DisplayRole> getLabUnitRoles() {
        return labUnitRoles;
    }

    public void setLabUnitRoles(List<DisplayRole> labUnitRoles) {
        this.labUnitRoles = labUnitRoles;
    }

    public List<String> getSelectedRoles() {
        return selectedRoles;
    }

    public void setSelectedRoles(List<String> selectedRoles) {
        this.selectedRoles = selectedRoles;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
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

    public String getAccountActive() {
        return accountActive;
    }

    public void setAccountActive(String accountActive) {
        this.accountActive = accountActive;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public Timestamp getSystemUserLastupdated() {
        return systemUserLastupdated;
    }

    public void setSystemUserLastupdated(Timestamp systemUserLastupdated) {
        this.systemUserLastupdated = systemUserLastupdated;
    }

    public List<IdValuePair> getTestSections() {
        return testSections;
    }

    public void setTestSections(List<IdValuePair> testSections) {
        this.testSections = testSections;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public List<String> getSelectedLabUnitRoles() {
        return selectedLabUnitRoles;
    }

    public void setSelectedLabUnitRoles(List<String> selectedLabUnitRoles) {
        this.selectedLabUnitRoles = selectedLabUnitRoles;
    }

    @JsonIgnore
    public JSONObject getUserLabRoleData() {
        return userLabRoleData;
    }

    public void setUserLabRoleData(JSONObject userLabRoleData) {
        this.userLabRoleData = userLabRoleData;
    }

    @JsonIgnore
    public JSONArray getSystemUsers() {
        return systemUsers;
    }

    public void setSystemUsers(JSONArray systemUsers) {
        this.systemUsers = systemUsers;
    }

    public String getSystemUserIdToCopy() {
        return systemUserIdToCopy;
    }

    public void setSystemUserIdToCopy(String systemUserIdToCopy) {
        this.systemUserIdToCopy = systemUserIdToCopy;
    }

    public String getAllowCopyUserRoles() {
        return allowCopyUserRoles;
    }

    public void setAllowCopyUserRoles(String allowCopyUserRoles) {
        this.allowCopyUserRoles = allowCopyUserRoles;
    }

    private Map<String, Set<String>> selectedTestSectionLabUnits = new HashMap<>();

    public Map<String, Set<String>> getSelectedTestSectionLabUnits() {
        return selectedTestSectionLabUnits;
    }

    public void setSelectedTestSectionLabUnits(Map<String, Set<String>> selectedTestSectionLabUnits) {
        this.selectedTestSectionLabUnits = selectedTestSectionLabUnits;
    }
}
