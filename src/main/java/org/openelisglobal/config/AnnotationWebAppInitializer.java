package org.openelisglobal.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.openelisglobal.common.servlet.barcode.LabelMakerServlet;
import org.openelisglobal.common.servlet.query.AjaxQueryXMLServlet;
import org.openelisglobal.common.servlet.reports.ReportsServlet;
import org.openelisglobal.common.servlet.validation.AjaxTextServlet;
import org.openelisglobal.common.servlet.validation.AjaxXMLServlet;
import org.openelisglobal.dataexchange.aggregatereporting.IndicatorAggregationReportingServlet;
import org.openelisglobal.dataexchange.order.action.OrderRawServlet;
import org.openelisglobal.dataexchange.order.action.OrderServlet;
import org.openelisglobal.metricservice.action.MetricServicesServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class AnnotationWebAppInitializer implements WebApplicationInitializer {

    int startupOrder = 0;

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);

        servletContext.addListener(new ContextLoaderListener(rootContext));

        setupServlets(servletContext, rootContext);
    }

    private void setupServlets(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext) {
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher",
                new DispatcherServlet(rootContext));
        dispatcher.setLoadOnStartup(++startupOrder);
        dispatcher.addMapping("/");
        // Enable Multipart Support
        dispatcher.setMultipartConfig(new jakarta.servlet.MultipartConfigElement(null, // Location
                10485760, // Max file size (10MB)
                20971520, // Max request size (20MB)
                0 // File size threshold
        ));

        // converted to controller
        // ServletRegistration.Dynamic logoUploadServlet =
        // servletContext.addServlet("logoUpload",
        // LogoUploadServlet.class);
        // logoUploadServlet.setLoadOnStartup(++startupOrder);
        // logoUploadServlet.addMapping("/logoUpload");

        ServletRegistration.Dynamic ajaxTextServlet = servletContext.addServlet("ajaxText", AjaxTextServlet.class);
        ajaxTextServlet.setLoadOnStartup(++startupOrder);
        ajaxTextServlet.addMapping("/ajaxText");

        ServletRegistration.Dynamic ajaxXMLServlet = servletContext.addServlet("ajaxXML", AjaxXMLServlet.class);
        ajaxXMLServlet.setLoadOnStartup(++startupOrder);
        ajaxXMLServlet.addMapping("/ajaxXML");

        ServletRegistration.Dynamic ajaxQueryXMLServlet = servletContext.addServlet("ajaxQueryXML",
                AjaxQueryXMLServlet.class);
        ajaxQueryXMLServlet.setLoadOnStartup(++startupOrder);
        ajaxQueryXMLServlet.addMapping("/ajaxQueryXML");

        // ServletRegistration.Dynamic ajaxAutocompleteXMLServlet =
        // servletContext.addServlet("ajaxAutocompleteXML",
        // org.openelisglobal.common.servlet.autocomplete.AjaxXMLServlet.class);
        // ajaxAutocompleteXMLServlet.setLoadOnStartup(++startupOrder);
        // ajaxAutocompleteXMLServlet.addMapping("/ajaxAutocompleteXML");

        // ServletRegistration.Dynamic ajaxSelectDropDownXMLServlet =
        // servletContext.addServlet("ajaxSelectDropDownXML",
        // org.openelisglobal.common.servlet.selectdropdown.AjaxXMLServlet.class);
        // ajaxSelectDropDownXMLServlet.setLoadOnStartup(++startupOrder);
        // ajaxSelectDropDownXMLServlet.addMapping("/ajaxSelectDropDownXML");

        ServletRegistration.Dynamic reportsServlet = servletContext.addServlet("reportsServlet", ReportsServlet.class);
        reportsServlet.setLoadOnStartup(++startupOrder);
        reportsServlet.addMapping("/reportsServlet");

        ServletRegistration.Dynamic ajaxDataXMLLServlet = servletContext.addServlet("ajaxDataXML",
                org.openelisglobal.common.servlet.data.AjaxXMLServlet.class);
        ajaxDataXMLLServlet.setLoadOnStartup(++startupOrder);
        ajaxDataXMLLServlet.addMapping("/ajaxDataXML");

        // ServletRegistration.Dynamic importAnalyzerServlet =
        // servletContext.addServlet("importAnalyzer",
        // AnalyzerImportServlet.class);
        // importAnalyzerServlet.setLoadOnStartup(++startupOrder);
        // importAnalyzerServlet.addMapping("/importAnalyzer");

        ServletRegistration.Dynamic metricServicesServlet = servletContext.addServlet("MetricServicesServlet",
                MetricServicesServlet.class);
        metricServicesServlet.setLoadOnStartup(++startupOrder);
        metricServicesServlet.addMapping("/MetricServices");

        ServletRegistration.Dynamic indicatorAggregationServlet = servletContext
                .addServlet("IndicatorAggregationServlet", IndicatorAggregationReportingServlet.class);
        indicatorAggregationServlet.setLoadOnStartup(++startupOrder);
        indicatorAggregationServlet.addMapping("/IndicatorAggregation");

        ServletRegistration.Dynamic orderServlet = servletContext.addServlet("OrderRequestServlet", OrderServlet.class);
        orderServlet.setLoadOnStartup(++startupOrder);
        orderServlet.addMapping("/OrderRequest");

        ServletRegistration.Dynamic orderRequestRawServlet = servletContext.addServlet("OrderRequestRawServlet",
                OrderRawServlet.class);
        orderRequestRawServlet.setLoadOnStartup(++startupOrder);
        orderRequestRawServlet.addMapping("/OrderRequest_Raw");

        ServletRegistration.Dynamic labelMakerServlet = servletContext.addServlet("LabelMakerServlet",
                LabelMakerServlet.class);
        labelMakerServlet.setLoadOnStartup(++startupOrder);
        labelMakerServlet.addMapping("/LabelMakerServlet");
    }
}
