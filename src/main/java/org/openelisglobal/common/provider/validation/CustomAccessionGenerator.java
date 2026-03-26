package org.openelisglobal.common.provider.validation;

import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.service.AccessionService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.spring.util.SpringContext;

public class CustomAccessionGenerator extends CustomAccessionValidator implements IAccessionNumberGenerator {

    protected AccessionService accessionService = SpringContext.getBean(AccessionService.class);

    @Override
    public String getNextAvailableAccessionNumber(String programCode, boolean reserve) {
        String nextAccessionNumber;
        do {
            nextAccessionNumber = getNextAccessionNumber(programCode, reserve);
        } while (accessionNumberIsUsed(nextAccessionNumber, null));
        return nextAccessionNumber;
    }

    @Override
    public String getNextAccessionNumber(String programCode, boolean reserve) {
        String template = ConfigurationProperties.getInstance().getPropertyValue(Property.CUSTOM_ACCESSION_TEMPLATE);
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("Custom accession template is not configured");
        }

        String staticPrefix = TemplateTokenEngine.getStaticPrefix(template);
        long nextNum;
        if (reserve) {
            nextNum = accessionService.getNextNumberIncrement(staticPrefix, AccessionFormat.CUSTOM);
        } else {
            nextNum = accessionService.getNextNumberNoIncrement(staticPrefix, AccessionFormat.CUSTOM);
        }

        return TemplateTokenEngine.processTokens(template, nextNum);
    }
}
