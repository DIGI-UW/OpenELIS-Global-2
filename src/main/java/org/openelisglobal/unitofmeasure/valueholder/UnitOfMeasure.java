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
package org.openelisglobal.unitofmeasure.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.localization.valueholder.Localization;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "UNIT_OF_MEASURE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class UnitOfMeasure extends EnumValueItemImpl {

    @Id
    @GeneratedValue(generator = "unit_of_measure_seq_gen")
    @GenericGenerator(name = "unit_of_measure_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "unit_of_measure_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "NAME", length = 20)
    private String unitOfMeasureName;

    @Column(name = "DESCRIPTION", length = 60)
    private String description;

    @Column(name = "CODE", length = 50)
    private String code;

    @Column(name = "UCUM_CODE", length = 50)
    private String ucumCode;

    @Transient
    private Localization localization;

    public UnitOfMeasure() {
        super();
    }

    public void setId(String id) {
        this.id = id;
        this.key = id;
    }

    @PostPersist
    @PostLoad
    private void syncEnumFields() {
        this.key = id;
        this.name = this.unitOfMeasureName;
    }

    public void setUnitOfMeasureName(String unitOfMeasureName) {
        this.unitOfMeasureName = unitOfMeasureName;
        this.name = unitOfMeasureName;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return getUnitOfMeasureName();
    }

    public Localization getLocalization() {
        // return (Localization)localization.getValue();
        //
        // UOM has been designed to support localization,
        // this method is the break point, to support localization
        // add columns to database table and Hibernation interface
        // then call localization.getValue above
        //

        Localization _localization = new Localization();
        _localization.setId(this.getId());
        _localization.setDescription(this.getDescription());
        _localization.setEnglish(this.getDefaultLocalizedName());
        _localization.setFrench("French");

        return (Localization) _localization;
    }

}
