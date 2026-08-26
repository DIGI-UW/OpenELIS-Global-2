package org.openelisglobal.microbiology.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroWhonetExportSelection implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("specimen")
    private List<String> specimen = new ArrayList<>();

    @JsonProperty("organism")
    private List<String> organism = new ArrayList<>();

    @JsonProperty("origin")
    private List<String> origin = new ArrayList<>();

    @JsonProperty("significance")
    private List<String> significance = new ArrayList<>();

    @JsonProperty("includeScreening")
    private boolean includeScreening;

    @JsonProperty("includeUnspecified")
    private boolean includeUnspecified;

    public MicroWhonetExportSelection() {
    }

    public MicroWhonetExportSelection(List<String> specimen, List<String> organism, List<String> origin,
            List<String> significance) {
        this(specimen, organism, origin, significance, false, false);
    }

    public MicroWhonetExportSelection(List<String> specimen, List<String> organism, List<String> origin,
            List<String> significance, boolean includeScreening, boolean includeUnspecified) {
        setSpecimen(specimen);
        setOrganism(organism);
        setOrigin(origin);
        setSignificance(significance);
        this.includeScreening = includeScreening;
        this.includeUnspecified = includeUnspecified;
    }

    public List<String> getSpecimen() {
        return List.copyOf(specimen);
    }

    public void setSpecimen(List<String> specimen) {
        this.specimen = copy(specimen);
    }

    public List<String> getOrganism() {
        return List.copyOf(organism);
    }

    public void setOrganism(List<String> organism) {
        this.organism = copy(organism);
    }

    public List<String> getOrigin() {
        return List.copyOf(origin);
    }

    public void setOrigin(List<String> origin) {
        this.origin = copy(origin);
    }

    public List<String> getSignificance() {
        return List.copyOf(significance);
    }

    public void setSignificance(List<String> significance) {
        this.significance = copy(significance);
    }

    public boolean isIncludeScreening() {
        return includeScreening;
    }

    public void setIncludeScreening(boolean includeScreening) {
        this.includeScreening = includeScreening;
    }

    public boolean isIncludeUnspecified() {
        return includeUnspecified;
    }

    public void setIncludeUnspecified(boolean includeUnspecified) {
        this.includeUnspecified = includeUnspecified;
    }

    private List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
