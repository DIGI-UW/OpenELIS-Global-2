package org.openelisglobal.coldstorage.service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FreezerExcursionData {
    private Long alertId;
    private Long freezerId;
    private String freezerName;
    private String locationName;
    private String startTime;
    private String endTime;
    private Long durationSeconds;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private String severity;
    private String status;
}
