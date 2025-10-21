package org.openelisglobal.locationreporting.controller;

import org.openelisglobal.locationreporting.form.LocationReportingForm;
import org.openelisglobal.locationreporting.service.LocationReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/locationreporting")
public class LocationReportingController {

    @Autowired
    private LocationReportingService locationReportingService;

    @GetMapping
    public LocationReportingForm getSettings() {
        LocationReportingForm form = new LocationReportingForm();
        form.setOptIn(locationReportingService.isOptedIn());
        return form;
    }

    @PostMapping
    public LocationReportingForm updateSettings(@RequestBody LocationReportingForm form) {
        locationReportingService.setOptIn(form.getOptIn());
        return form;
    }

    @PostMapping("/send")
    public void sendReportNow() {
        locationReportingService.sendLocationReport();
    }
}
