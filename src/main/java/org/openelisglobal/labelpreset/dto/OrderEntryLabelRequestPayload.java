package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for {@code POST /api/orderEntry/labelRequest} (OGC-285 M5).
 * Mirrors {@code OrderEntryLabelRequestPayload} in
 * {@code contracts/openapi.yaml} §8.1. Pure read input — the candidate order's
 * test ids + samples — used to compute the Order Entry Labels section column
 * set and per-cell defaults. Carries NO chosen quantities (those come back in
 * the response defaults; the persist payload is a separate DTO).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEntryLabelRequestPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("test_ids")
    private List<Long> testIds = new ArrayList<>();

    @JsonProperty("samples")
    private List<SampleRef> samples = new ArrayList<>();

    public OrderEntryLabelRequestPayload() {
    }

    public List<Long> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<Long> testIds) {
        this.testIds = testIds;
    }

    public List<SampleRef> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleRef> samples) {
        this.samples = samples;
    }

    /** A client-supplied candidate sample (local id + sample-type code). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SampleRef implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("sample_id_local")
        private String sampleIdLocal;

        @JsonProperty("sample_type")
        private String sampleType;

        public SampleRef() {
        }

        public String getSampleIdLocal() {
            return sampleIdLocal;
        }

        public void setSampleIdLocal(String sampleIdLocal) {
            this.sampleIdLocal = sampleIdLocal;
        }

        public String getSampleType() {
            return sampleType;
        }

        public void setSampleType(String sampleType) {
            this.sampleType = sampleType;
        }
    }
}
