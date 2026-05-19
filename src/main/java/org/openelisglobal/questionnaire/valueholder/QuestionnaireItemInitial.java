package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Date;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_item_initial")
public class QuestionnaireItemInitial extends BaseObject<String> {

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "questionnaire_item_initial_seq_gen")
    @GenericGenerator(name = "questionnaire_item_initial_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "questionnaire_item_initial_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_item_id", nullable = false)
    private QuestionnaireItem questionnaireItem;

    @Column(name = "value_string", length = 1000)
    private String valueString;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_integer")
    private Integer valueInteger;

    @Column(name = "value_decimal")
    private Double valueDecimal;

    @Column(name = "value_date")
    private Date valueDate;

    public QuestionnaireItemInitial() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public QuestionnaireItem getQuestionnaireItem() {
        return questionnaireItem;
    }

    public void setQuestionnaireItem(QuestionnaireItem questionnaireItem) {
        this.questionnaireItem = questionnaireItem;
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

    public Double getValueDecimal() {
        return valueDecimal;
    }

    public void setValueDecimal(Double valueDecimal) {
        this.valueDecimal = valueDecimal;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }
}