package org.openelisglobal.common.provider.validation;

public interface IAccessionNumberGenerator extends IAccessionNumberValidator {

    /**
     * @param programCode -- if used, may be null otherwise
     * @return The first accession number if no others are have been generated
     */
    // String createFirstAccessionNumber(String programCode);

    // String incrementAccessionNumber(String currentHighAccessionNumber);

    /**
     * @param programCode -- code if needed, may be null
     * @param reserve     -- whether it should reserve this code (normally true)
     * @return The next available number, may be null if one can not be generated.
     */
    String getNextAvailableAccessionNumber(String programCode, boolean reserve);

    String getNextAccessionNumber(String programCode, boolean reserve);
}
