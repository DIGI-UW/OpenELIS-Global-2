package org.openelisglobal.profile.form;

import org.openelisglobal.common.form.BaseForm;
import org.springframework.web.multipart.MultipartFile;

public class ProfileForm extends BaseForm {

    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
