package org.openelisglobal.inventory.service;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * A physical count: somebody stood at the shelf and said what was actually
 * there.
 *
 * <p>
 * A count is recorded per lot, because stock only exists as lots in this module
 * — an item's on-hand is the sum of them, so there is no item-level quantity to
 * write a count against without guessing which lot a discrepancy belongs to.
 *
 * <p>
 * Counting a lot and adjusting it are not the same event. A count that agrees
 * with the record still happened and is still worth knowing about, so it stamps
 * the item as counted and writes no adjustment. Only a discrepancy moves stock.
 */
public interface InventoryCountService {

    /**
     * Commits one count session.
     *
     * <p>
     * Every entry given is a lot somebody actually counted; nothing is inferred
     * about the lots they did not. The whole session is one transaction: it applies
     * completely or not at all, so an abandoned or failed count cannot leave half
     * the shelf adjusted.
     *
     * @return what the session did, including the reference every adjustment it
     *         wrote is grouped under
     */
    CountResult recordCount(List<CountEntry> entries, String sysUserId);

    /** One counted lot: what it is, and what was on the shelf. */
    @Getter
    @Setter
    class CountEntry {
        private Long lotId;
        private Double countedQuantity;
    }

    /** What one confirmed count session did. */
    @Getter
    class CountResult {
        /**
         * The session's own number, written onto every adjustment it made as
         * {@code reference_id} with {@code reference_type = ADJUSTMENT}. Null when the
         * count found no discrepancy at all, because then there is nothing to group.
         */
        private final Long sessionReference;

        /** Lots whose counted quantity differed from the record, and were moved. */
        private final int adjusted;

        /** Lots counted and found correct. Stamped, not adjusted. */
        private final int confirmed;

        public CountResult(Long sessionReference, int adjusted, int confirmed) {
            this.sessionReference = sessionReference;
            this.adjusted = adjusted;
            this.confirmed = confirmed;
        }
    }
}
