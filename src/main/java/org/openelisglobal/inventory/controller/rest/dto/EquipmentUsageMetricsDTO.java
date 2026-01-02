package org.openelisglobal.inventory.controller.rest.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EquipmentUsageMetricsDTO
 *
 * Aggregated usage statistics for the equipment usage dashboard. Provides
 * top-level metrics and breakdowns by equipment and lab unit.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentUsageMetricsDTO {

    private Integer totalEquipmentCount;
    private Integer totalUsageRecords;
    private List<EquipmentUsageStat> usageByEquipment;

    /**
     * Per-equipment usage statistics
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentUsageStat {
        private Long equipmentId;
        private String equipmentName;
        private Integer usageCount;
        private Double totalQuantityUsed;
    }
}
