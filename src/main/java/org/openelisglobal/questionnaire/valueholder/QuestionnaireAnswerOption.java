package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_answer_option")
public class QuestionnaireAnswerOption extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questionnaire_answer_option_generator")
    @SequenceGenerator(name = "questionnaire_answer_option_generator", sequenceName = "questionnaire_answer_option_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_item_id", nullable = false)
    private QuestionnaireItem questionnaireItem;

    @Column(name = "value_code", length = 100)
    private String valueCode;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "initial_selected")
    private boolean initialSelected;

    public QuestionnaireAnswerOption() {
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public QuestionnaireItem getQuestionnaireItem() {
        return questionnaireItem;
    }

    public void setQuestionnaireItem(QuestionnaireItem questionnaireItem) {
        this.questionnaireItem = questionnaireItem;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isInitialSelected() {
        return initialSelected;
    }

    public void setInitialSelected(boolean initialSelected) {
        this.initialSelected = initialSelected;
    }
}