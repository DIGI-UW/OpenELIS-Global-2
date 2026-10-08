package org.openelisglobal.inventory.imports;

public interface InventoryImportService {

    String template();

    InventoryImportPlan preview(String csv);

    InventoryImportPlan apply(String csv, String sysUserId);
}
