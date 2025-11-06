package org.openelisglobal.dataexchange.fhir.controller;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.itech.fhir.dataexport.api.service.DataExportService;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dataexport/fhir")
public class FhirExportController {

    @Autowired
    private DataExportService dataExportService;
    @Autowired
    private DataExportTaskService dataExportTaskService;

    @PostMapping
    public void runAllDataExportTasks() throws InterruptedException, ExecutionException, TimeoutException {
        for (DataExportTask dataExportTask : dataExportTaskService.getDAO().findAll()) {
            dataExportService.exportNewDataFromLocalToRemote(dataExportTask);
        }
    }
}
