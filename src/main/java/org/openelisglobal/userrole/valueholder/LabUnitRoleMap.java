package org.openelisglobal.userrole.valueholder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * LabUnitRoleMap represents a Map of Map<String , List<String>> ie <labUnit ,
 * List<roles>>
 */
@Entity
@Table(name = "lab_unit_role_map")
public class LabUnitRoleMap {

    @Id
    @GeneratedValue
    @Column(name = "lab_unit_role_map_id")
    private Integer id;

    @Column(name = "lab_unit")
    private String labUnit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lab_roles", joinColumns = {
            @JoinColumn(name = "lab_unit_role_map_id", referencedColumnName = "lab_unit_role_map_id") })
    @Column(name = "role")
    Set<String> roles;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabUnit() {
        return labUnit;
    }

    public void setLabUnit(String labUnit) {
        this.labUnit = labUnit;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}