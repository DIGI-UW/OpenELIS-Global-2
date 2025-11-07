package org.openelisglobal.odoo.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;

@Converter(autoApply = false)
public class SyncStatusConverter implements AttributeConverter<SyncStatus, String> {

    @Override
    public String convertToDatabaseColumn(SyncStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SyncStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SyncStatus.valueOf(dbData);
    }
}
