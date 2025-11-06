package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_experiment")
public class AnalyzerExperiment extends BaseObject<Integer> {

    private static final long serialVersionUID = -219455306834962412L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analyzer_experiment_generator")
    @SequenceGenerator(name = "analyzer_experiment_generator", sequenceName = "analyzer_experiment_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Valid
    @OneToOne
    @JoinColumn(name = "analyzer_id", referencedColumnName = "id")
    private Analyzer analyzer;

    @Column(name = "name")
    private String name;

    @Column(name = "file")
    private byte[] file;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }
}
