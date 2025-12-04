package org.openelisglobal.alert.form;

import lombok.Data;

/**
 * Lightweight DTO for Freezer information in alerts
 */
@Data
public class FreezerDTO {
    private Long id;
    private String name;
    private String code;
}
