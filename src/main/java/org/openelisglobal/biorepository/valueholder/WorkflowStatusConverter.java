package org.openelisglobal.biorepository.valueholder;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;

/**
 * JPA converter for BioSample.WorkflowStatus enum. Converts between enum values
 * and database string representation.
 */
@Converter(autoApply = false)
public class WorkflowStatusConverter implements AttributeConverter<WorkflowStatus, String> {

    @Override
    public String convertToDatabaseColumn(WorkflowStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public WorkflowStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return WorkflowStatus.fromString(dbData);
    }
}
