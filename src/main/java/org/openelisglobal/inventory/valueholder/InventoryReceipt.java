package org.openelisglobal.inventory.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.organization.valueholder.Organization;

/*
 * N.B.  This is a subset of the columns in the table.
 * If you want more then add them here and in the mapping file
 */
public class InventoryReceipt extends BaseObject<String> {

    private static final long serialVersionUID = 1L;
    private String id;
    private String inventoryItemId;
    private Timestamp receivedDate;
    private ValueHolderInterface organization = new ValueHolder();
    private String orgId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Timestamp receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setOrganization(ValueHolderInterface organization) {
        this.organization = organization;
    }

    public void setOrganizationHolder(ValueHolderInterface organization) {
        this.organization = organization;
    }

    public void setOrganization(Organization organization) {
        this.organization.setValue(organization);
    }

    public ValueHolderInterface getOrganizationHolder() {
        return organization;
    }

    public Organization getOrganization() {
        return (Organization) organization.getValue();
    }

    public void setInventoryItemId(String inventoryItem) {
        this.inventoryItemId = inventoryItem;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }
}
