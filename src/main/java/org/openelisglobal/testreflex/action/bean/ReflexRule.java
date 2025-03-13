package org.openelisglobal.testreflex.action.bean;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "reflex_rule")
public class ReflexRule extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reflex_rule_generator")
    @SequenceGenerator(name = "reflex_rule_generator", sequenceName = "reflex_rule_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "rule_name")
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall")
    private ReflexRuleOptions.OverallOptions overall;

    @Column(name = "toggled")
    private Boolean toggled;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "reflex_rule_id", referencedColumnName = "id")
    Set<ReflexRuleCondition> conditions;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "reflex_rule_id", referencedColumnName = "id")
    Set<ReflexRuleAction> actions;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "analyte_id")
    private Integer analyteId;

    @Transient
    String localizedName;

    @Transient
    String stringId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public ReflexRuleOptions.OverallOptions getOverall() {
        return overall;
    }

    public void setOverall(ReflexRuleOptions.OverallOptions overall) {
        this.overall = overall;
    }

    public Boolean getToggled() {
        return toggled;
    }

    public void setToggled(Boolean toggled) {
        this.toggled = toggled;
    }

    public Set<ReflexRuleCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<ReflexRuleCondition> conditions) {
        this.conditions = conditions;
    }

    public Set<ReflexRuleAction> getActions() {
        return actions;
    }

    public void setActions(Set<ReflexRuleAction> actions) {
        this.actions = actions;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public void setStringId(String stringId) {
        this.stringId = stringId;
    }

    public Integer getAnalyteId() {
        return analyteId;
    }

    public void setAnalyteId(Integer analyteId) {
        this.analyteId = analyteId;
    }
}
