/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.organization.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "ORGANIZATION_TYPE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class OrganizationType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "organization_type_seq_gen")
    @GenericGenerator(name = "organization_type_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "organization_type_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "SHORT_NAME", length = 20, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 60, nullable = true)
    private String description;

    @Column(name = "hierarchy_level", nullable = true)
    private Integer hierarchyLevel;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "organization_organization_type", joinColumns = @JoinColumn(name = "org_type_id"), inverseJoinColumns = @JoinColumn(name = "org_id"))
    private Set<Organization> organizations;

    @Column(name = "name_display_key", length = 60)
    private String nameKey;

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return name;
    }

}
