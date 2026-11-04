package org.openelisglobal.microbiology.form;

public class MicroContainerLabelForm {
    public record Preset(Integer id, String name, Integer defaultQuantity, Integer maxQuantity) {
    }

    public String kind, caseId, rowId;
    public Integer presetId;
    public int quantity = 1;

    public record Target(String caseId, String rowId, String scope) {
    }
}
