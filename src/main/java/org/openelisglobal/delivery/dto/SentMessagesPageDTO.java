package org.openelisglobal.delivery.dto;

import java.util.ArrayList;
import java.util.List;

public class SentMessagesPageDTO {

    private List<SentMessageDTO> rows = new ArrayList<>();
    private long total;
    private int page;
    private int pageSize;

    public List<SentMessageDTO> getRows() {
        return rows;
    }

    public void setRows(List<SentMessageDTO> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
