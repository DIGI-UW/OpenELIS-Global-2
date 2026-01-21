package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk sample registration endpoint.
 */
public class BulkRegistrationResponse {

    private boolean success;
    private String error;
    private int registeredCount;
    private List<RegisteredSample> samples = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(int registeredCount) {
        this.registeredCount = registeredCount;
    }

    public List<RegisteredSample> getSamples() {
        return samples;
    }

    public void setSamples(List<RegisteredSample> samples) {
        this.samples = samples;
    }

    public void addSample(RegisteredSample sample) {
        this.samples.add(sample);
        this.registeredCount++;
    }

    /**
     * Information about a registered sample.
     */
    public static class RegisteredSample {
        private Integer id;
        private String barcode;
        private Integer sampleItemId;
        private Integer sampleId;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public Integer getSampleItemId() {
            return sampleItemId;
        }

        public void setSampleItemId(Integer sampleItemId) {
            this.sampleItemId = sampleItemId;
        }

        public Integer getSampleId() {
            return sampleId;
        }

        public void setSampleId(Integer sampleId) {
            this.sampleId = sampleId;
        }
    }
}
