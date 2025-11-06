/**
 * The contents of this file are subject to the Mozilla License Version 1.1 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
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
package org.openelisglobal.common.provider.validation;

/**
 * @author paulsc
 */
public interface IAccessionNumberValidator {

    enum ValidationResults {
        SUCCESS, SITE_FAIL, YEAR_FAIL, USED_FAIL, IS_NOT_USED_FAIL, LENGTH_FAIL, FORMAT_FAIL, PROGRAM_FAIL,
        REQUIRED_FAIL, PATIENT_STATUS_FAIL, SAMPLE_STATUS_FAIL, SAMPLE_FOUND, SAMPLE_NOT_FOUND, GENERIC_FAIL
    }

    /**
     * @return does this accession number have a program code as part of it
     */
    boolean needProgramCode();

    /**
     * @param accessionNumber -- The number to be checked
     * @param checkDate       -- true if there is a date element in the number which
     *                        should be checked or false otherwise If the number is
     *                        one which has already been entered into the system
     *                        then it would make sense to set this to false since
     *                        when the year turns over all of the sudden they will
     *                        fail.
     * @return One of the possible results for validation
     * @throws IllegalArgumentException
     */
    ValidationResults validFormat(String accessionNumber, boolean checkDate) throws IllegalArgumentException;

    /**
     * Helper method for getting an appropriate message for a validation result
     *
     * @param results -- the result for which the message is wanted
     * @return -- the message
     */
    String getInvalidMessage(ValidationResults results);

    /**
     * Helper method for getting an appropriate message for a format validation
     * result
     *
     * @param results -- the result for which the message is wanted
     * @return -- the message
     */
    String getInvalidFormatMessage(ValidationResults results);

    int getMaxAccessionLength();

    int getMinAccessionLength();

    boolean accessionNumberIsUsed(String accessionNumber, String recordType);

    ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String projectFormName);

    /**
     * Get the part of the accession number which should not change. ie. for Haiti
     * it would be the site number, for Cote d'Ivoire it would be the Program prefix
     *
     * @return
     */
    int getInvarientLength();

    /**
     * The max length - the invarientLength
     *
     * @return
     */
    int getChangeableLength();

    String getPrefix();
}
