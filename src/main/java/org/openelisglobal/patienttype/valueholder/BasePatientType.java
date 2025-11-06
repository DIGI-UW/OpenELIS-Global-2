package org.openelisglobal.patienttype.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;

/**
 * This is an object that contains data related to the patient_type table. Do
 * not modify this class because it will be overwritten if the configuration
 * file related to this class is modified.
 *
 * @hibernate.class table="patient_type"
 */
public abstract class BasePatientType extends EnumValueItemImpl {

    private static final long serialVersionUID = -7636195859201443397L;
    public static final String REF = "PatientType";
    public static final String PROP_TYPE = "type";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_LASTUPDATED = "lastupdated";
    public static final String PROP_ID = "id";

    // constructors
    public BasePatientType() {
        initialize();
    }

    /** Constructor for primary key */
    public BasePatientType(String id) {
        this.setId(id);
        initialize();
    }

    protected void initialize() {
    }

    private int hashCodeValue = Integer.MIN_VALUE;

    // primary key
    private String id;

    // fields
    private String type;
    private String description;

    /**
     * Return the unique identifier of this class
     *
     * @hibernate.id generator-class="sequence" column="id"
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Set the unique identifier of this class
     *
     * @param id the new ID
     */
    @Override
    public void setId(String id) {
        this.id = id;
        this.hashCodeValue = Integer.MIN_VALUE;
    }

    /** Return the value associated with the column: type */
    public String getType() {
        return type;
    }

    /**
     * Set the value related to the column: type
     *
     * @param type the type value
     */
    public void setType(String type) {
        this.type = type;
    }

    /** Return the value associated with the column: description */
    public String getDescription() {
        return description;
    }

    /**
     * Set the value related to the column: description
     *
     * @param description the description value
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (!(obj instanceof PatientType)) {
            return false;
        } else {
            PatientType patientType = (PatientType) obj;
            if (this == patientType) {
                return true;
            } else if (null == this.getId() || null == patientType.getId()) {
                return false;
            } else {
                return (this.getId().equals(patientType.getId()));
            }
        }
    }

    @Override
    public int hashCode() {
        if (Integer.MIN_VALUE == this.hashCodeValue) {
            if (null == this.getId()) {
                return super.hashCode();
            } else {
                String hashStr = this.getClass().getSimpleName() + ":" + this.getId().hashCode();
                this.hashCodeValue = hashStr.hashCode();
            }
        }
        return this.hashCodeValue;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
