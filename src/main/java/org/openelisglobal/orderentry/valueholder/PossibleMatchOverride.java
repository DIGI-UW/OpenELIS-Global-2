package org.openelisglobal.orderentry.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * The record of Create new anyway (FRS clinical order entry v4, FR-B6a, D-216):
 * who created a new patient, facility or provider although similar records were
 * shown, when ({@code last_updated}), what was entered and which matches were
 * shown, both as JSON text.
 */
@Entity
@Table(name = "possible_match_override")
public class PossibleMatchOverride extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "possible_match_override_generator")
    @SequenceGenerator(name = "possible_match_override_generator", sequenceName = "possible_match_override_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "record_kind", nullable = false, length = 20)
    private String recordKind;

    @Column(name = "entered_values")
    private String enteredValues;

    @Column(name = "matches_shown")
    private String matchesShown;

    @Column(name = "sys_user_id")
    private Integer recordedById;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getRecordKind() {
        return recordKind;
    }

    public void setRecordKind(String recordKind) {
        this.recordKind = recordKind;
    }

    public String getEnteredValues() {
        return enteredValues;
    }

    public void setEnteredValues(String enteredValues) {
        this.enteredValues = enteredValues;
    }

    public String getMatchesShown() {
        return matchesShown;
    }

    public void setMatchesShown(String matchesShown) {
        this.matchesShown = matchesShown;
    }

    public Integer getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(Integer recordedById) {
        this.recordedById = recordedById;
    }
}
