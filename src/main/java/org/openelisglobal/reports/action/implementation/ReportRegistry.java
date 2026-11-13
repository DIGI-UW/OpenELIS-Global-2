
package org.openelisglobal.reports.action.implementation;

import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ReportRegistry {

    @Autowired
    private ApplicationContext context;

    public IReportCreator getReportCreator(String reportName) {
        if (GenericValidator.isBlankOrNull(reportName)) {
            return null;
        }

        IReportCreator creator = getBeanIfTypeMatches(reportName, IReportCreator.class);

        if (creator != null) {
            return creator;
        }

        return getBeanIfTypeMatches(reportName + "Creator", IReportCreator.class);
    }

    public IReportParameterSetter getParameterSetter(String reportName) {
        if (GenericValidator.isBlankOrNull(reportName)) {
            return null;
        }

        return getBeanIfTypeMatches(reportName, IReportParameterSetter.class);
    }

    private <T> T getBeanIfTypeMatches(String beanName, Class<T> beanType) {
        if (context.containsBean(beanName) && context.isTypeMatch(beanName, beanType)) {
            return context.getBean(beanName, beanType);
        }

        return null;
    }
}
