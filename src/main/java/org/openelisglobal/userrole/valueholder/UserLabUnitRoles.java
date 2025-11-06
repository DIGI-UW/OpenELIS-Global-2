package org.openelisglobal.userrole.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "user_lab_unit_roles")
public class UserLabUnitRoles extends BaseObject<Integer> {

    // set as the systemUser
    @Id
    @Column(name = "system_user_id")
    private Integer id;

    // we could do it simply by Map<String , List<String>> ie <labUnit ,
    // List<roles>> but there are issues mapping that with hibernate
    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @JoinTable(name = "lab_unit_roles", joinColumns = @JoinColumn(name = "system_user_id"), inverseJoinColumns = @JoinColumn(name = "lab_unit_role_map_id"))
    private Set<LabUnitRoleMap> labUnitRoleMap;

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public Set<LabUnitRoleMap> getLabUnitRoleMap() {
        return labUnitRoleMap;
    }

    public void setLabUnitRoleMap(Set<LabUnitRoleMap> labUnitRoleMap) {
        this.labUnitRoleMap = labUnitRoleMap;
    }
}
