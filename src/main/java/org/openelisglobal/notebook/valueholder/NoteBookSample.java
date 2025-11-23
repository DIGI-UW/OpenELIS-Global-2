package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@Entity
@Table(name = "notebook_samples")
public class NoteBookSample extends BaseObject<Integer> {

    private static final long serialVersionUID = -979624722823577193L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_samples_generator")
    @SequenceGenerator(name = "notebook_samples_generator", sequenceName = "notebook_samples_seq", allocationSize = 1)
    private Integer id;

    @Valid
    @NotNull
    @ManyToOne
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    private NoteBook notebook;

    @Valid
    @NotNull
    @ManyToOne
    @JoinColumn(name = "sample_item_id", referencedColumnName = "id")
    private SampleItem sampleItem;

    @Column(name = "questionnaire_response_uuid")
    private UUID questionnaireResponseUuid;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }
}
