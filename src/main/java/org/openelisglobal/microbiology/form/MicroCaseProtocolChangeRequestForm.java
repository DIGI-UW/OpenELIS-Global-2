package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseProtocolChangeRequestForm {

    @NotBlank
    @Size(max = 20)
    public String cultureMethodId;

    @NotBlank
    @Size(max = 255)
    public String reason;
}
