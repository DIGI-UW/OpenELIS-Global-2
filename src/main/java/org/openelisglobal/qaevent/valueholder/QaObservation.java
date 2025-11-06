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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.qaevent.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;

public class QaObservation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public enum ObservedType {
        SAMPLE("SAMPLE"), ANALYSIS("ANALYSIS");

        String dbName;

        private ObservedType(String type) {
            dbName = type;
        }

        public String getDBName() {
            return dbName;
        }
    }

    private String id;
    private String observedId;
    private String observedType;
    private ValueHolder observationType;
    private String valueType;
    private String value;

    public QaObservation() {
        observationType = new ValueHolder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObservedId() {
        return observedId;
    }

    public void setObservedId(String observedId) {
        this.observedId = observedId;
    }

    public String getObservedType() {
        return observedType;
    }

    public void setObservedType(String observedType) {
        this.observedType = observedType;
    }

    public QaObservationType getObservationType() {
        return (QaObservationType) observationType.getValue();
    }

    public void setObservationType(QaObservationType type) {
        observationType.setValue(type);
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
