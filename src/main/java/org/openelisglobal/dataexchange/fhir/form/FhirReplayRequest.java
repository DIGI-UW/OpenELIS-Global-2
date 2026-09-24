package org.openelisglobal.dataexchange.fhir.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class FhirReplayRequest {

    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotNull @Pattern(regexp = "[1-9][0-9]{0,9}") @Max(Integer.MAX_VALUE) String> sampleIds;

    public List<String> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<String> sampleIds) {
        this.sampleIds = sampleIds;
    }
}
