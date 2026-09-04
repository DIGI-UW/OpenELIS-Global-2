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
package org.openelisglobal.program.valueholder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.TestSection;

@Setter
@Getter
@Entity
@Table(name = "PROGRAM")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
@JsonAutoDetect(fieldVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
public class Program extends BaseObject<String> {

    @JsonProperty("code")
    @Pattern(regexp = "(?i)^[a-z0-9_ ]*$")
    @Column(name = "CODE", precision = 10, nullable = false)
    private String code;

    @Id
    @JsonProperty("id")
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @GeneratedValue(generator = "program_seq_gen")
    @GenericGenerator(name = "program_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "program_seq"))
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @JsonProperty("programName")
    @Pattern(regexp = "(?i)^[a-z0-9-_ ]*$")
    @Column(name = "NAME", length = 50, nullable = false)
    private String programName;

    @JsonProperty("questionnaireUUID")
    @Column(name = "questionnaire_fhir_uuid")
    private UUID questionnaireUUID;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "test_section_id")
    private TestSection testSection;

    @JsonProperty("manuallyChanged")
    @Column(name = "manually_changed", length = 1, nullable = false)
    private Boolean manuallyChanged;

    public Program() {
        super();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}
