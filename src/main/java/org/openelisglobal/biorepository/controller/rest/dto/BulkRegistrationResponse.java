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
    private int failedCount;
    private List<RegisteredSample> samples = new ArrayList<>();
    private List<String> rowErrors = new ArrayList<>();

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

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<RegisteredSample> getSamples() {
        return samples;
    }

    public void setSamples(List<RegisteredSample> samples) {
        this.samples = samples;
    }

    public List<String> getRowErrors() {
        return rowErrors;
    }

    public void setRowErrors(List<String> rowErrors) {
        this.rowErrors = rowErrors;
    }

    public void addSample(RegisteredSample sample) {
        this.samples.add(sample);
        this.registeredCount++;
    }

    public void addRowError(String rowError) {
        this.rowErrors.add(rowError);
        this.failedCount++;
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
