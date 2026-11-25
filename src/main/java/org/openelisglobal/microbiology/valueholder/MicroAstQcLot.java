package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_qc_lot", schema = "clinlims")
public class MicroAstQcLot extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "control_id", nullable = false, length = 36)
    private String controlId;

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String value) {
        controlId = value;
    }

    @Column(name = "inventory_lot_id", nullable = false)
    private Long inventoryLotId;

    public Long getInventoryLotId() {
        return inventoryLotId;
    }

    public void setInventoryLotId(Long value) {
        inventoryLotId = value;
    }
}
