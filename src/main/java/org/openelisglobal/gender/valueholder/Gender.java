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
 */
package org.openelisglobal.gender.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "gender")
public class Gender extends BaseObject<Integer> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gender_generator")
    @SequenceGenerator(name = "gender_generator", sequenceName = "gender_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "description")
    @Size(max = 20)
    private String description;

    @Column(name = "gender_type")
    @Size(max = 1)
    private String genderType;

    @Column(name = "name_key")
    @Size(max = 60)
    private String nameKey;

    public Gender() {
        super();
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenderType() {
        return genderType;
    }

    public void setGenderType(String genderType) {
        this.genderType = genderType;
    }

    @Override
    public String getNameKey() {
        return this.nameKey;
    }

    @Override
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    @Override
    public String getDefaultLocalizedName() {
        return this.description;
    }

    @Override
    public String toString() {
        return "Gender { Id = " + id + ", description=" + description + " }";
    }
}
