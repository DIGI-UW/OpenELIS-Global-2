package org.openelisglobal.privilege.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Privilege - Represents a fine-grained system privilege.
 *
 * <p>
 * Maps to clinlims.system_privilege. Privileges are assigned to roles via
 * system_role_privilege and resolved transitively for users through their role
 * memberships.
 */
@Entity
@Table(name = "system_privilege", schema = "clinlims")
public class Privilege extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_privilege_seq_gen")
    @SequenceGenerator(name = "system_privilege_seq_gen", sequenceName = "system_privilege_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Getters and Setters

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
