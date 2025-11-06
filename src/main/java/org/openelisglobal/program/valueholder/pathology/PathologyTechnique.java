package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "pathology_technique")
public class PathologyTechnique extends BaseObject<Integer> {

    public enum TechniqueType {
        DICTIONARY("D"), TEXT("T");

        private String code;

        TechniqueType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        static TechniqueType fromCode(String code) {
            if (code.equals("D")) {
                return DICTIONARY;
            }
            if (code.equals("T")) {
                return TEXT;
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_technique_generator")
    @SequenceGenerator(name = "pathology_technique_generator", sequenceName = "pathology_technique_seq", allocationSize = 1)
    private Integer id;

    private String value;

    @Enumerated(EnumType.STRING)
    private TechniqueType type;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TechniqueType getType() {
        return type;
    }

    public void setType(TechniqueType type) {
        this.type = type;
    }
}
