package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryTag;

public interface InventoryTagService extends BaseObjectService<InventoryTag, Long> {

    /**
     * One row per tag, whether it has a directory entry or not, with usage counts.
     */
    List<TagSummary> getDirectory();

    /** Tags that may still be suggested and filtered on, alphabetically. */
    List<String> getActiveTagNames();

    /** Creates a tag ahead of use, or reactivates it if it was deactivated. */
    TagSummary createTag(String name, String sysUserId);

    /**
     * Stops a tag being suggested or offered as a filter. Items carrying it are
     * untouched — that is the whole point of deactivating rather than deleting.
     */
    void setActive(String name, boolean active, String sysUserId);

    /** A tag, how many items carry it, and whether it is still offered. */
    class TagSummary {
        private final String name;
        private final long itemCount;
        private final boolean active;

        public TagSummary(String name, long itemCount, boolean active) {
            this.name = name;
            this.itemCount = itemCount;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public long getItemCount() {
            return itemCount;
        }

        public boolean isActive() {
            return active;
        }
    }
}
