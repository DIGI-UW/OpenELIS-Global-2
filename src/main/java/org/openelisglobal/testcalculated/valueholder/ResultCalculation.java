package org.openelisglobal.testcalculated.valueholder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;

@Entity
@Table(name = "result_calculation")
public class ResultCalculation extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_calculation_generator")
    @SequenceGenerator(name = "result_calculation_generator", sequenceName = "result_calculation_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "calculation_id")
    private Calculation calculation;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @OneToOne
    @JoinColumn(name = "result_id")
    private Result result;

    @ManyToMany
    @JoinTable(name = "test_operations", joinColumns = @JoinColumn(name = "result_calculation_id"), inverseJoinColumns = @JoinColumn(name = "test_id"))
    private Set<Test> test;

    /**
     * The result feeding each operand of the calculation, keyed by operand id.
     *
     * <p>
     * Keyed by test id, one multi-component test could only occupy a single slot,
     * so whichever component's result was written last became the value the
     * calculation read. The operand is the thing that names a measurement — it
     * carries the test, the specimen and the component — so it is what the slot
     * belongs to.
     */
    @ElementCollection
    @CollectionTable(name = "test_result_map", joinColumns = @JoinColumn(name = "result_calculation_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "operation_id")
    @Column(name = "result_id")
    private Map<Integer, Integer> operandResultMap;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Calculation getCalculation() {
        return calculation;
    }

    public void setCalculation(Calculation calculation) {
        this.calculation = calculation;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Set<Test> getTest() {
        return test;
    }

    public void setTest(Set<Test> test) {
        this.test = test;
    }

    public Map<Integer, Integer> getOperandResultMap() {
        return operandResultMap;
    }

    public void setOperandResultMap(Map<Integer, Integer> operandResultMap) {
        this.operandResultMap = operandResultMap;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
