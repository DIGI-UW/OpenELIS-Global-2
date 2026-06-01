package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Bulk suggestion request for fulfillment workbench rows.
 */
public class RetrievalItemSuggestionsRequestDTO {

    private List<Integer> itemIds = new ArrayList<>();
    private List<RetrievalItemIdentityLookupDTO> identityLookups = new ArrayList<>();

    public List<Integer> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Integer> itemIds) {
        this.itemIds = itemIds != null ? itemIds : new ArrayList<>();
    }

    public List<RetrievalItemIdentityLookupDTO> getIdentityLookups() {
        return identityLookups;
    }

    public void setIdentityLookups(List<RetrievalItemIdentityLookupDTO> identityLookups) {
        this.identityLookups = identityLookups != null ? identityLookups : new ArrayList<>();
    }
}
