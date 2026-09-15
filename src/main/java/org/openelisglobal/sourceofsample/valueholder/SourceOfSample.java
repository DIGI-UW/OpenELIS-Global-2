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
package org.openelisglobal.sourceofsample.valueholder;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@DynamicUpdate
@Entity
@Table(name = "SOURCE_OF_SAMPLE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class SourceOfSample extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "src_of_sample_seq_gen")
    @GenericGenerator(name = "src_of_sample_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "src_of_sample_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "DESCRIPTION", length = 20)
    private String description;

    @Column(name = "DOMAIN", length = 1)
    private String domain;

    public SourceOfSample() {
        super();
    }

}
