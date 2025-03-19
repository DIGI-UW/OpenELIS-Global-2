package org.openelisglobal.program.valueholder.cytology;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "cytology_specimen_adequacy")
public class CytologySpecimenAdequacy extends BaseObject<Integer> {

    public enum SpecimenAdequacyResultType {
        DICTIONARY("D"), TEXT("T");

        private String code;

        SpecimenAdequacyResultType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        static SpecimenAdequacyResultType fromCode(String code) {
            if (code.equals("D")) {
                return DICTIONARY;
            }
            if (code.equals("T")) {
                return TEXT;
            }
            return null;
        }
    }

    public enum SpecimenAdequancySatisfaction {
        SATISFACTORY_FOR_EVALUATION("Satisfactory for evaluation"),
        UN_SATISFACTORY_FOR_EVALUATION("Un Satisfactory for evaluation");

        private String display;

        SpecimenAdequancySatisfaction(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cytology_specimen_adequacy_generator")
    @SequenceGenerator(name = "cytology_specimen_adequacy_generator", sequenceName = "cytology_specimen_adequacy_seq", allocationSize = 1)
    private Integer id;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "cytology_specimen_adequacy_value", joinColumns = @JoinColumn(name = "cytology_specimen_adequacy_id"))
    @Column(name = "value", nullable = false)
    private List<String> values;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type")
    private SpecimenAdequacyResultType resultType = SpecimenAdequacyResultType.DICTIONARY;

    @Enumerated(EnumType.STRING)
    @Column(name = "satisfaction")
    private SpecimenAdequancySatisfaction satisfaction;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SpecimenAdequacyResultType getResultType() {
        return resultType;
    }

    public void setResultType(SpecimenAdequacyResultType resultType) {
        this.resultType = resultType;
    }

    public SpecimenAdequancySatisfaction getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(SpecimenAdequancySatisfaction satisfaction) {
        this.satisfaction = satisfaction;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
