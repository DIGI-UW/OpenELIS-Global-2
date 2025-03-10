package org.openelisglobal.program.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

@Entity
@Table(name = "program_sample")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ProgramSample extends BaseObject<Integer> {

    private static final long serialVersionUID = -979624722823577192L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "program_sample_generator")
    @SequenceGenerator(name = "program_sample_generator", sequenceName = "program_sample_seq", allocationSize = 1)
    private Integer id;

    @Valid
    @NotNull
    @OneToOne
    @JoinColumn(name = "program_id", referencedColumnName = "id")
    private Program program;

    @Valid
    @NotNull
    @OneToOne
    @JoinColumn(name = "sample_id", referencedColumnName = "id")
    private Sample sample;

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

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }
}
