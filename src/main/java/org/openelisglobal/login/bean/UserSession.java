package org.openelisglobal.login.bean;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserSession {

    public enum LoginMethod {
        FORM, SAML, OAUTH, CERT, NONE
    }

    private Boolean authenticated;
    private LoginMethod loginMethod;
    private String sessionId;
    private String userId;
    private String loginName;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Map<String, List<String>> userLabRolesMap;
    private String CSRF;
    private String loginLabUnit;

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }

    public LoginMethod getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(LoginMethod loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCSRF() {
        return CSRF;
    }

    public void setCSRF(String cSRF) {
        CSRF = cSRF;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, List<String>> getUserLabRolesMap() {
        return userLabRolesMap;
    }

    public void setUserLabRolesMap(Map<String, List<String>> userLabRolesMap) {
        this.userLabRolesMap = userLabRolesMap;
    }

    public String getLoginLabUnit() {
        return loginLabUnit;
    }

    public void setLoginLabUnit(String loginLabUnit) {
        this.loginLabUnit = loginLabUnit;
    }
}
