package org.openelisglobal.patient.merge.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * DTO containing summary of data to be consolidated during patient merge.
 * Includes counts of orders, results, samples, documents, and conflicting
 * fields.
 */
@Data
public class PatientMergeDataSummaryDTO {

    private int totalOrders;
    private int activeOrders;
    private int totalResults;
    private int totalSamples;
    private int totalDocuments;
    private int totalIdentifiers;
    private int totalContacts;
    private int totalRelations;
    private int totalAuditEntries;

    private List<String> conflictingFields = new ArrayList<>();
    private List<String> conflictingIdentityTypes = new ArrayList<>();

    /**
     * Adds a conflicting field name to the summary.
     */
    public void addConflictingField(String fieldName) {
        this.conflictingFields.add(fieldName);
    }

    /**
     * Adds a conflicting identity type to the summary.
     */
    public void addConflictingIdentityType(String identityType) {
        this.conflictingIdentityTypes.add(identityType);
    }

    /**
     * Checks if there are any conflicting fields.
     */
    public boolean hasConflicts() {
        return !conflictingFields.isEmpty() || !conflictingIdentityTypes.isEmpty();
    }

    /**
     * Gets total data items across all categories.
     */
    public int getTotalDataItems() {
        return totalOrders + totalResults + totalSamples + totalDocuments + totalIdentifiers;
    }
}
