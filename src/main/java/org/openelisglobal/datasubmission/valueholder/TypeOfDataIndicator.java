package org.openelisglobal.datasubmission.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class TypeOfDataIndicator extends BaseObject<String> {
    public enum Type {
        TAT("Turnaround Time"), VLCoverage("VL Coverage"), Trends("Testing Trends");

        private String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return this.getValue();
        }

        public static Type getEnum(String value) {
            for (Type type : values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private String id;
    private String name;
    private String description;
    private String descriptionKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }
}
