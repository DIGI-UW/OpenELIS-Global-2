package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryTag;

public interface InventoryTagService extends BaseObjectService<InventoryTag, Long> {

    /** Tags in use plus tags with only a directory row, one entry per tag. */
    List<TagSummary> getDirectory();

    /** Sorted case-insensitively. */
    List<String> getActiveTagNames();

    /** Creates a tag ahead of use, or reactivates it if it was deactivated. */
    TagSummary createTag(String name, String sysUserId);

    /** Inactive tags are no longer offered; items keep them. */
    void setActive(String name, boolean active, String sysUserId);

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
