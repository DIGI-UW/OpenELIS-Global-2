/**
 * Project : LIS<br>
 * File name : PatientType.java<br>
 * Description : Patienttype
 *
 * @author TienDH
 * @date Nov 20, 2007
 */
package org.openelisglobal.patienttype.valueholder;

public class PatientType extends BasePatientType {
    private static final long serialVersionUID = 1L;

    public PatientType() {
        super();
    }

    /** Constructor for primary key */
    public PatientType(String id) {
        super(id);
    }
}
