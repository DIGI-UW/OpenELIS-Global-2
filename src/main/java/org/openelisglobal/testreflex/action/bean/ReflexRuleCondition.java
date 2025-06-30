package org.openelisglobal.testreflex.action.bean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "reflex_rule_condition")
public class ReflexRuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reflex_rule_condition_generator")
    @SequenceGenerator(name = "reflex_rule_condition_generator", sequenceName = "reflex_rule_condition_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sample_id")
    private String sampleId;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "test_id")
    private String testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation")
    private ReflexRuleOptions.NumericRelationOptions relation;

    @Column(name = "value")
    private String value;

    @Column(name = "value2")
    private String value2;

    @Column(name = "test_analyte_id")
    private Integer testAnalyteId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public ReflexRuleOptions.NumericRelationOptions getRelation() {
        return relation;
    }

    public void setRelation(ReflexRuleOptions.NumericRelationOptions relation) {
        this.relation = relation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public Integer getTestAnalyteId() {
        return testAnalyteId;
    }

    public void setTestAnalyteId(Integer testAnalyteId) {
        this.testAnalyteId = testAnalyteId;
    }
}
