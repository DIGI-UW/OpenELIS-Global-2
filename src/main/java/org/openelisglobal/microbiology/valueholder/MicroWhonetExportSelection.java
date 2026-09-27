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

    @JsonProperty("dedupBasis")
    private String dedupBasis = "COLLECTION_DATE";

    @JsonProperty("dedupScope")
    private String dedupScope = "ANY_SOURCE";

    @JsonProperty("excludeContaminants")
    private boolean excludeContaminants = true;

    @JsonProperty("profileSensitivity")
    private String profileSensitivity = "INSENSITIVE";

    public MicroWhonetExportSelection() {
    }

    public MicroWhonetExportSelection(List<String> specimen, List<String> organism, List<String> origin,
            List<String> significance) {
        this(specimen, organism, origin, significance, false, false);
    }

    public MicroWhonetExportSelection(List<String> specimen, List<String> organism, List<String> origin,
            List<String> significance, boolean includeScreening, boolean includeUnspecified) {
        this(specimen, organism, origin, significance, includeScreening, includeUnspecified, "COLLECTION_DATE",
                "ANY_SOURCE", true, "INSENSITIVE");
    }

    public MicroWhonetExportSelection(List<String> specimen, List<String> organism, List<String> origin,
            List<String> significance, boolean includeScreening, boolean includeUnspecified, String dedupBasis,
            String dedupScope, boolean excludeContaminants, String profileSensitivity) {
        setSpecimen(specimen);
        setOrganism(organism);
        setOrigin(origin);
        setSignificance(significance);
        this.includeScreening = includeScreening;
        this.includeUnspecified = includeUnspecified;
        this.dedupBasis = dedupBasis;
        this.dedupScope = dedupScope;
        this.excludeContaminants = excludeContaminants;
        this.profileSensitivity = profileSensitivity;
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

    public String getDedupBasis() {
        return dedupBasis;
    }

    public void setDedupBasis(String dedupBasis) {
        this.dedupBasis = dedupBasis;
    }

    public String getDedupScope() {
        return dedupScope;
    }

    public void setDedupScope(String dedupScope) {
        this.dedupScope = dedupScope;
    }

    public boolean isExcludeContaminants() {
        return excludeContaminants;
    }

    public void setExcludeContaminants(boolean excludeContaminants) {
        this.excludeContaminants = excludeContaminants;
    }

    public String getProfileSensitivity() {
        return profileSensitivity;
    }

    public void setProfileSensitivity(String profileSensitivity) {
        this.profileSensitivity = profileSensitivity;
    }

    private List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
