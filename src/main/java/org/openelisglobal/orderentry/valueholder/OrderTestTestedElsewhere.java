package org.openelisglobal.orderentry.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A test on an order whose result another laboratory reported (FRS clinical
 * order entry v4, FR-B20): the performing laboratory, from the Organizations
 * list, and the reported value. One row per order and test.
 */
@Entity
@Table(name = "order_test_tested_elsewhere", uniqueConstraints = {
        @UniqueConstraint(name = "order_test_tested_elsewhere_uniq", columnNames = { "sample_id", "test_id" }) })
public class OrderTestTestedElsewhere extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_test_tested_elsewhere_generator")
    @SequenceGenerator(name = "order_test_tested_elsewhere_generator", sequenceName = "order_test_tested_elsewhere_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sample_id", nullable = false)
    private Integer sampleId;

    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @Column(name = "performing_lab_id")
    private Integer performingLabId;

    @Column(name = "reported_value", length = 255)
    private String reportedValue;

    @Column(name = "sys_user_id")
    private Integer recordedById;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getPerformingLabId() {
        return performingLabId;
    }

    public void setPerformingLabId(Integer performingLabId) {
        this.performingLabId = performingLabId;
    }

    public String getReportedValue() {
        return reportedValue;
    }

    public void setReportedValue(String reportedValue) {
        this.reportedValue = reportedValue;
    }

    public Integer getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(Integer recordedById) {
        this.recordedById = recordedById;
    }
}
