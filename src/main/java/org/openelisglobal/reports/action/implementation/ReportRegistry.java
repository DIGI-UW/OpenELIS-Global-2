package org.openelisglobal.reports.action.implementation;

import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.BeansException;
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

        try {
            return context.getBean(reportName, IReportCreator.class);
        } catch (BeansException e) {
            try {
                return context.getBean(reportName + "Creator", IReportCreator.class);
            } catch (BeansException ex) {
                return null;
            }
        }
    }

    public IReportParameterSetter getParameterSetter(String reportName) {
        if (GenericValidator.isBlankOrNull(reportName)) {
            return null;
        }

        try {
            return context.getBean(reportName, IReportParameterSetter.class);
        } catch (BeansException e) {
            return null;
        }
    }
}
