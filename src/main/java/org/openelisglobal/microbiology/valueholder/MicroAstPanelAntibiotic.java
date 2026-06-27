package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_panel_antibiotic", schema = "clinlims")
public class MicroAstPanelAntibiotic extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "panel_id", nullable = false, length = 36)
    private String panelId;

    @Column(name = "antibiotic_id", nullable = false, length = 36)
    private String antibioticId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public String getAntibioticId() {
        return antibioticId;
    }

    public void setAntibioticId(String antibioticId) {
        this.antibioticId = antibioticId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
