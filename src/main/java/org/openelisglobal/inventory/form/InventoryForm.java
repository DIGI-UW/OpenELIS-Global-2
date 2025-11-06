package org.openelisglobal.inventory.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.validation.annotations.ValidDate;

public class InventoryForm extends BaseForm {

    public interface ManageInventory {
    }

    @ValidDate(relative = DateRelation.TODAY)
    private String currentDate = "";

    @NotNull(groups = { ManageInventory.class })
    @Valid
    private List<InventoryKitItem> inventoryItems;

    // for display
    private List<IdValuePair> sources;

    // for display
    private List<String> kitTypes;

    // in display
    private String newKitsXML = "";

    public InventoryForm() {
        setFormName("InventoryForm");
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public List<InventoryKitItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryKitItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public List<IdValuePair> getSources() {
        return sources;
    }

    public void setSources(List<IdValuePair> sources) {
        this.sources = sources;
    }

    public List<String> getKitTypes() {
        return kitTypes;
    }

    public void setKitTypes(List<String> kitTypes) {
        this.kitTypes = kitTypes;
    }

    public String getNewKitsXML() {
        return newKitsXML;
    }

    public void setNewKitsXML(String newKitsXML) {
        this.newKitsXML = newKitsXML;
    }
}
