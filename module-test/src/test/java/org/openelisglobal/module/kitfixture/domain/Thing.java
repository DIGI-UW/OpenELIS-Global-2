package org.openelisglobal.module.kitfixture.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mod_kitfixture_thing", schema = "clinlims")
public class Thing {

    @Id
    private Long id;

    public Long getId() {
        return id;
    }
}
