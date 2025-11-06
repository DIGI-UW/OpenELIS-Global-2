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

    private String id;
    private String observationHistoryTypeId;
    private String patientId;
    private String sampleId;
    private String value;
    private String valueType;
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

    public String getObservationHistoryTypeId() {
        return this.observationHistoryTypeId;
    }

    public void setObservationHistoryTypeId(String observationHistoryTypeId) {
        this.observationHistoryTypeId = observationHistoryTypeId;
    }

    public String getPatientId() {
        return this.patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getSampleId() {
        return this.sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType.getCode();
    }

    /**
     * value type indicates whether the value is a literal ("L") or the value is an
     * index to a fixed value from Dictionary ("D")
     *
     * @return "L" or "D"
     */
    public String getValueType() {
        return valueType;
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

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }
}
