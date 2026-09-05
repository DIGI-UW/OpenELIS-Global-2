/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.observationhistory.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.SimpleBaseEntity;

/**
 * Entities which represent facts about a patient and a sample that has entered
 * the lab. Often taken from a demographic survey form filled out by the doctor,
 * but sometimes something which is generated in the lab like various statuses
 * of a sample. Originally for Cote d'Ivoire RetroCI Project
 *
 * @author Paul A. Hill
 * @since 2010-04-16
 */

@Setter
@Getter
@Entity
@Table(name = "observation_history")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class ObservationHistory extends BaseObject<String> implements SimpleBaseEntity<String> {
    private static final long serialVersionUID = 1L;

    /** A Definition of all the correct types for the valueType field. */
    public enum ValueType {
        DICTIONARY("D"), // the value is a FK to Dictionary.id
        LITERAL("L"), // the value is simply a literal value
        KEY("K"); // the value is a key for localization

        private String code;

        ValueType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    // Fields

    @Id
    @GeneratedValue(generator = "observation_history_seq_gen")
    @GenericGenerator(name = "observation_history_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "observation_history_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;

    @Column(name = "observation_history_type_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String observationHistoryTypeId;

    @Column(name = "patient_id", precision = 10, scale = 0, nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String patientId;

    @Column(name = "sample_id", precision = 10, scale = 0, nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleId;

    @Column(name = "VALUE", length = 40)
    private String value;

    @Column(name = "VALUE_TYPE", length = 1)
    private String valueType;

    @Column(name = "sample_item_id", precision = 10, scale = 0, nullable = true)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleItemId;

    // Constructors

    /** default constructor */
    public ObservationHistory() {
    }

    /** minimal constructor */
    public ObservationHistory(String id, String patientId, String sampleId) {
        this.id = id;
        this.patientId = patientId;
        this.sampleId = sampleId;
    }

    /** full constructor */
    public ObservationHistory(String id, String observationHistoryTypeId, String patientId, String sampleId,
            String value, ValueType valueType) {
        this.id = id;
        this.observationHistoryTypeId = observationHistoryTypeId;
        this.patientId = patientId;
        this.sampleId = sampleId;
        this.value = value;
        this.valueType = valueType.getCode();
    }

    // Property accessors

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType.getCode();
    }

    @Override
    public String toString() {
        return "ObservationHistory [ObservationHistoryTypeId=" + observationHistoryTypeId + ", id=" + id
                + ", patientId=" + patientId + ", sampleId=" + sampleId + ", value=" + value + ", valueType="
                + valueType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((observationHistoryTypeId == null) ? 0 : observationHistoryTypeId.hashCode());
        result = prime * result + ((patientId == null) ? 0 : patientId.hashCode());
        result = prime * result + ((sampleId == null) ? 0 : sampleId.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ObservationHistory other = (ObservationHistory) obj;
        if (observationHistoryTypeId == null) {
            if (other.observationHistoryTypeId != null) {
                return false;
            }
        } else if (!observationHistoryTypeId.equals(other.observationHistoryTypeId)) {
            return false;
        }
        if (patientId == null) {
            if (other.patientId != null) {
                return false;
            }
        } else if (!patientId.equals(other.patientId)) {
            return false;
        }
        if (sampleId == null) {
            if (other.sampleId != null) {
                return false;
            }
        } else if (!sampleId.equals(other.sampleId)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        if (valueType == null) {
            if (other.valueType != null) {
                return false;
            }
        } else if (!valueType.equals(other.valueType)) {
            return false;
        }
        return true;
    }
}
