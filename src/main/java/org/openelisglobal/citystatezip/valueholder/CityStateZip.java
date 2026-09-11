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
package org.openelisglobal.citystatezip.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "CITY_STATE_ZIP")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class CityStateZip extends BaseObject<String> {

    @Id
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "CITY", length = 30)
    private String city;

    @Column(name = "STATE", length = 2)
    private String state;

    @Column(name = "ZIP_CODE", length = 10)
    private String zipCode;

    @Column(name = "COUNTY_FIPS", length = 3)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String countyFips;

    @Column(name = "COUNTY", length = 25)
    private String county;

    @Column(name = "REGION_ID", length = 3)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String regionId;

    @Column(name = "REGION", length = 30)
    private String region;

    @Column(name = "STATE_FIPS", length = 3)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String stateFips;

    @Column(name = "STATE_NAME", length = 30)
    private String stateName;

    public CityStateZip() {
        super();
    }

}
