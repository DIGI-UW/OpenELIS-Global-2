package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.List;

public class MicroWhonetExportQueryForm {

    public String from;
    public String to;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> specimen = new ArrayList<>();
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> organism = new ArrayList<>();
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> origin = new ArrayList<>();
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> significance = new ArrayList<>(List.of("CLINICALLY_SIGNIFICANT"));
    public boolean includeScreening;
    public boolean includeUnspecified;
    public String dedup = "FIRST_ISOLATE_7_DAY";
    public String dedupBasis = "COLLECTION_DATE";
    public String dedupScope = "ANY_SOURCE";
    public boolean excludeContaminants = true;
    public String profileSensitivity = "INSENSITIVE";
    public int page = 1;
    public int pageSize = 20;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<String> getSpecimen() {
        return specimen;
    }

    public void setSpecimen(List<String> specimen) {
        this.specimen = specimen;
    }

    public List<String> getOrganism() {
        return organism;
    }

    public void setOrganism(List<String> organism) {
        this.organism = organism;
    }

    public List<String> getOrigin() {
        return origin;
    }

    public void setOrigin(List<String> origin) {
        this.origin = origin;
    }

    public List<String> getSignificance() {
        return significance;
    }

    public void setSignificance(List<String> significance) {
        this.significance = significance;
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

    public String getDedup() {
        return dedup;
    }

    public void setDedup(String dedup) {
        this.dedup = dedup;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
