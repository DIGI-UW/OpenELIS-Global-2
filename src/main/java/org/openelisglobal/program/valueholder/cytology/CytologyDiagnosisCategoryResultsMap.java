package org.openelisglobal.program.valueholder.cytology;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import org.openelisglobal.common.hibernateConverter.StringListConverter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.DiagnosisCategory;

@Entity
@Table(name = "cytology_diagnosis_result_map")
public class CytologyDiagnosisCategoryResultsMap extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cytology_diagnosis_result_map_generator")
    @SequenceGenerator(name = "cytology_diagnosis_result_map_generator", sequenceName = "cytology_diagnosis_result_map_seq", allocationSize = 1)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private DiagnosisCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type")
    private CytologyDiagnosisResultType resultType;

    @Convert(converter = StringListConverter.class)
    @Column(name = "results")
    private List<String> results;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public List<String> getResults() {
        return results;
    }

    public void setResults(List<String> results) {
        this.results = results;
    }

    public CytologyDiagnosisResultType getResultType() {
        return resultType;
    }

    public void setResultType(CytologyDiagnosisResultType resultType) {
        this.resultType = resultType;
    }

    public DiagnosisCategory getCategory() {
        return category;
    }

    public void setCategory(DiagnosisCategory category) {
        this.category = category;
    }
}
