package org.openelisglobal.labunit.dto;

import java.util.List;

/**
 * Paginated response wrapper for Lab Unit list endpoints. Provides metadata for
 * client-side pagination controls.
 */
public class LabUnitListResponse {

    private List<LabUnitResponse> items;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public LabUnitListResponse() {
    }

    public LabUnitListResponse(List<LabUnitResponse> items, int currentPage, int totalPages, long totalItems,
            int pageSize) {
        this.items = items;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
        this.pageSize = pageSize;
        this.hasNext = currentPage < totalPages;
        this.hasPrevious = currentPage > 1;
    }

    public List<LabUnitResponse> getItems() {
        return items;
    }

    public void setItems(List<LabUnitResponse> items) {
        this.items = items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}