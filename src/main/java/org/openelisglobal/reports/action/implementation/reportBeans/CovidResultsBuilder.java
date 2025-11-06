package org.openelisglobal.reports.action.implementation.reportBeans;

public interface CovidResultsBuilder {

    void buildDataSource();

    byte[] getDataSourceAsByteArray();
}
