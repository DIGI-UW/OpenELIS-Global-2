package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroContainerLabelForm;

public interface MicroContainerLabelService {
    java.util.List<MicroContainerLabelForm.Preset> presets();

    MicroContainerLabelForm.Target resolve(String code, String actor);

    byte[] print(MicroContainerLabelForm request, String actor);
}
