package org.openelisglobal.reportqueue.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * The queue settings a user keeps between visits: the time window and the page
 * size.
 */
@Entity
@Table(name = "report_queue_preference", schema = "clinlims")
public class ReportQueuePreference extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_WINDOW = "7d";
    public static final int DEFAULT_PAGE_SIZE = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "system_user_id", nullable = false, unique = true)
    private Long systemUserId;

    @Column(name = "time_window", nullable = false, length = 16)
    private String timeWindow = DEFAULT_WINDOW;

    @Column(name = "page_size", nullable = false)
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Long systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
