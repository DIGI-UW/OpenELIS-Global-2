package org.openelisglobal.login.form;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class LoginForm extends BaseForm {
    @NotBlank
    @ValidName(nameType = NameType.USERNAME, message = "username is invalid")
    private String loginName = "";

    @NotBlank
    private String password;

    private Boolean useSAML;

    private Map<String, String> oauthUrls;

    public LoginForm() {
        setFormName("loginForm");
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getUseSAML() {
        return useSAML;
    }

    public void setUseSAML(Boolean useSAML) {
        this.useSAML = useSAML;
    }

    public Map<String, String> getOauthUrls() {
        return oauthUrls;
    }

    public void setOauthUrls(Map<String, String> oauthUrls) {
        this.oauthUrls = oauthUrls;
    }
}
