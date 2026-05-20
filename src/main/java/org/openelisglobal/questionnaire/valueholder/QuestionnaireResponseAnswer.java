package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_response_answer")
public class QuestionnaireResponseAnswer extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questionnaire_response_answer_generator")
    @SequenceGenerator(name = "questionnaire_response_answer_generator", sequenceName = "questionnaire_response_answer_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_response_item_id", nullable = false)
    private QuestionnaireResponseItem item;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_integer")
    private Integer valueInteger;

    @Column(name = "value_decimal")
    private BigDecimal valueDecimal;

    @Column(name = "value_date")
    private Date valueDate;

    @Column(name = "value_datetime")
    private Timestamp valueDateTime;

    @Column(name = "coding_code")
    private String codingCode;

    @Column(name = "coding_system")
    private String codingSystem;

    @Column(name = "coding_display")
    private String codingDisplay;

    @Column(name = "value_reference")
    private String valueReference;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public QuestionnaireResponseItem getItem() {
        return item;
    }

    public void setItem(QuestionnaireResponseItem item) {
        this.item = item;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public Boolean getValueBoolean() {
        return valueBoolean;
    }

    public void setValueBoolean(Boolean valueBoolean) {
        this.valueBoolean = valueBoolean;
    }

    public Integer getValueInteger() {
        return valueInteger;
    }

    public void setValueInteger(Integer valueInteger) {
        this.valueInteger = valueInteger;
    }

    public BigDecimal getValueDecimal() {
        return valueDecimal;
    }

    public void setValueDecimal(BigDecimal valueDecimal) {
        this.valueDecimal = valueDecimal;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Timestamp getValueDateTime() {
        return valueDateTime;
    }

    public void setValueDateTime(Timestamp valueDateTime) {
        this.valueDateTime = valueDateTime;
    }

    public String getCodingCode() {
        return codingCode;
    }

    public void setCodingCode(String codingCode) {
        this.codingCode = codingCode;
    }

    public String getCodingSystem() {
        return codingSystem;
    }

    public void setCodingSystem(String codingSystem) {
        this.codingSystem = codingSystem;
    }

    public String getCodingDisplay() {
        return codingDisplay;
    }

    public void setCodingDisplay(String codingDisplay) {
        this.codingDisplay = codingDisplay;
    }

    public String getValueReference() {
        return valueReference;
    }

    public void setValueReference(String valueReference) {
        this.valueReference = valueReference;
    }
}