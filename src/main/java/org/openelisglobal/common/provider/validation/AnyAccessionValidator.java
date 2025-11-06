package org.openelisglobal.common.provider.validation;

public class AnyAccessionValidator implements IAccessionNumberGenerator {

    @Override
    public boolean needProgramCode() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ValidationResults validFormat(String accessionNumber, boolean checkDate) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInvalidMessage(ValidationResults results) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInvalidFormatMessage(ValidationResults results) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNextAvailableAccessionNumber(String programCode, boolean reserve) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMaxAccessionLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMinAccessionLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean accessionNumberIsUsed(String accessionNumber, String recordType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ValidationResults checkAccessionNumberValidity(String accessionNumber, String recordType, String isRequired,
            String projectFormName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getInvarientLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getChangeableLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNextAccessionNumber(String programCode, boolean reserve) {
        // TODO Auto-generated method stub
        return null;
    }
}
