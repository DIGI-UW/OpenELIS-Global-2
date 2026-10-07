package org.openelisglobal.samplebatchentry.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

/**
 * The body of a Batch Order Entry save: an order-entry form plus, for the EID
 * study form, the specimens and test the technician ticked on the setup screen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleBatchEntrySaveForm extends SamplePatientEntryForm {

    /** Flags are null when the client sends an untouched (blank) checkbox. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EidSelection {
        private Boolean dryTubeTaken;
        private Boolean dbsTaken;
        private Boolean dnaPCR;

        public boolean isDryTubeTaken() {
            return Boolean.TRUE.equals(dryTubeTaken);
        }

        public void setDryTubeTaken(Boolean dryTubeTaken) {
            this.dryTubeTaken = dryTubeTaken;
        }

        public boolean isDbsTaken() {
            return Boolean.TRUE.equals(dbsTaken);
        }

        public void setDbsTaken(Boolean dbsTaken) {
            this.dbsTaken = dbsTaken;
        }

        public boolean isDnaPCR() {
            return Boolean.TRUE.equals(dnaPCR);
        }

        public void setDnaPCR(Boolean dnaPCR) {
            this.dnaPCR = dnaPCR;
        }
    }

    @JsonProperty("_ProjectDataEID")
    private EidSelection eidSelection;

    @JsonProperty("_ProjectDataEID")
    public EidSelection getEidSelection() {
        return eidSelection;
    }

    @JsonProperty("_ProjectDataEID")
    public void setEidSelection(EidSelection eidSelection) {
        this.eidSelection = eidSelection;
    }
}
