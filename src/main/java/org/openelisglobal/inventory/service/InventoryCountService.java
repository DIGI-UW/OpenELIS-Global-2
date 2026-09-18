package org.openelisglobal.inventory.service;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public interface InventoryCountService {

    /** All or nothing; lots not listed are left untouched. */
    CountResult recordCount(List<CountEntry> entries, String sysUserId);

    @Getter
    @Setter
    class CountEntry {
        private Long lotId;
        private Double countedQuantity;
    }

    @Getter
    class CountResult {
        /** reference_id on the session's adjustments; null when none were made. */
        private final Long sessionReference;

        private final int adjusted;

        private final int confirmed;

        public CountResult(Long sessionReference, int adjusted, int confirmed) {
            this.sessionReference = sessionReference;
            this.adjusted = adjusted;
            this.confirmed = confirmed;
        }
    }
}
