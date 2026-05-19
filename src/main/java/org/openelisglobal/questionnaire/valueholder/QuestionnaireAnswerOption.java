package org.openelisglobal.questionnaire.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "questionnaire_answer_option")
public class QuestionnaireAnswerOption extends BaseObject<String> {

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "questionnaire_answer_option_seq_gen")
    @GenericGenerator(name = "questionnaire_answer_option_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "questionnaire_answer_option_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

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