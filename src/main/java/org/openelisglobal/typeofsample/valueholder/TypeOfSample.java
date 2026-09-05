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
package org.openelisglobal.typeofsample.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.valueholder.Localization;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "TYPE_OF_SAMPLE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class TypeOfSample extends BaseObject<String> {

    /** */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "type_of_sample_seq_gen")
    @GenericGenerator(name = "type_of_sample_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "type_of_sample_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "DESCRIPTION", length = 20, nullable = false)
    private String description;

    @Column(name = "DOMAIN", length = 20)
    private String domain;

    @Column(name = "LOCAL_ABBREV", length = 10, unique = true)
    private String localAbbreviation;

    @Column(name = "WHONET_CODE", length = 5)
    private String whonetCode;

    @Column(name = "display_key", length = 60)
    private String nameKey;

    @Column(name = "disposal_instructions")
    private String disposalInstructions;

    @Column(name = "is_active", length = 1)
    private boolean isActive;

    @Column(name = "sort_order")
    private int sortOrder;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "name_localization_id")
    private Localization localization = new Localization();

    public TypeOfSample() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    protected String getDefaultLocalizedName() {
        String msg = "";
        try {
            msg = getLocalization().getLocalizedValue();
            return msg;
        } catch (RuntimeException e) {
            // throw away
            return "TypeOfSample:getDefaultLocalizedName:84:";
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TypeOfSample that = (TypeOfSample) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
