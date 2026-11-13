package org.openelisglobal.dashboard.management.quality;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dashboard.management.DashboardPermissions;
import org.openelisglobal.qa.security.QaPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/dashboards/management/quality-weights")
public class QiWeightsRestController extends BaseRestController {

    @Autowired
    private QiWeightsService qiWeightsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(DashboardPermissions.VIEW_MANAGEMENT + " or " + QaPermissions.MANAGE_QI)
    public WeightsView getWeights() {
        return qiWeightsService.getWeights();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(QaPermissions.MANAGE_QI)
    public ResponseEntity<Void> saveWeights(@RequestBody WeightsView weights, HttpServletRequest request) {
        try {
            qiWeightsService.saveWeights(weights, ControllerUtills.getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }
}
