package org.openelisglobal.privilege.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * RolePrivilege - Junction entity linking a system role to a privilege.
 *
 * <p>
 * Maps to clinlims.system_role_privilege. The system_role_id column is stored
 * as a plain Integer rather than a {@code @ManyToOne} to Role because Role uses
 * legacy HBM XML mapping with {@code LIMSStringNumberUserType}, which is
 * incompatible with JPA {@code @JoinColumn}.
 */
@Entity
@Table(name = "system_role_privilege", schema = "clinlims")
public class RolePrivilege extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_role_privilege_seq_gen")
    @SequenceGenerator(name = "system_role_privilege_seq_gen", sequenceName = "system_role_privilege_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /**
     * Plain column mapping — NOT a @ManyToOne to Role. Role uses legacy HBM XML
     * with LIMSStringNumberUserType; a JPA @JoinColumn would break the mapping. The
     * value is the numeric PK of clinlims.system_role.
     */
    @Column(name = "system_role_id")
    private Integer roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id")
    private Privilege privilege;

    // Getters and Setters

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Privilege getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }
}
