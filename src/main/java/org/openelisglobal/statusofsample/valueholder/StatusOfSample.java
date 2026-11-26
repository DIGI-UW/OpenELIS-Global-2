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
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.i
 */
package org.openelisglobal.statusofsample.valueholder;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;

/**
 * @author bill mcgough bugzilla 1625
 */
@Getter
@Setter
@DynamicUpdate
@Entity
@Table(name = "STATUS_OF_SAMPLE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class StatusOfSample extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "status_of_sample_seq_gen")
    @GenericGenerator(name = "status_of_sample_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "status_of_sample_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "DESCRIPTION", length = 240)
    private String description;

    @Column(name = "NAME", length = 30, nullable = false)
    private String statusOfSampleName;

    @Column(name = "CODE", length = 3)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String code;

    @Column(name = "STATUS_TYPE", length = 10)
    private String statusType;

    @Column(name = "is_active")
    private String isActive;

    public StatusOfSample() {
        super();
    }

    public void setDescription(String description) {
        this.description = description;
        // bugzilla 1625
        this.name = description;
    }

    public void setId(String id) {
        this.id = id;
        // bugzilla 1625
        this.key = id;
    }

    @Override
    public String getNameKey() {
        return super.getNameKey();
    }

    @Override
    public void setNameKey(String nameKey) {
        super.setNameKey(nameKey);
    }

    public String getDefaultLocalizedName() {
        return getStatusOfSampleName();
    }

}
