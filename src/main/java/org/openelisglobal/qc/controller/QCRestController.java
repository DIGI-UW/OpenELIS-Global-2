package org.openelisglobal.qc.controller;

import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.form.QCControlLotForm;
import org.openelisglobal.qc.form.WestgardRuleConfigForm;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCDashboardService;
import org.openelisglobal.qc.service.QCDashboardService.DashboardSummary;
import org.openelisglobal.qc.service.QCDashboardService.InstrumentComplianceStatus;
import org.openelisglobal.qc.service.QCStatisticsService;
import org.openelisglobal.qc.service.WestgardRuleConfigService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for QC Control Lot management. Supports User Story 6: Manage
 * QC Control Lots Following Constitution IV.5: @Transactional in services ONLY
 * (NOT controllers)
 */
@RestController
@RequestMapping("/rest/qc")
public class QCRestController {

    private static final String[] ALLOWED_FIELDS = new String[] { "id", "productName", "lotNumber", "manufacturer",
            "controlLevel", "testId", "instrumentId", "calculationMethod", "initialRunsCount", "manufacturerMean",
            "manufacturerStdDev", "activationDate", "expirationDate", "status", "unitOfMeasure", "internalNotes",
            "externalNotes" };

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private QCStatisticsService statisticsService;

    @Autowired
    private WestgardRuleConfigService ruleConfigService;

    @Autowired
    private QCDashboardService dashboardService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    /**
     * Get all active control lots for a specific test and instrument. GET
     * /rest/qc/controlLots?testId={testId}&instrumentId={instrumentId}
     */
    @GetMapping("/controlLots")
    public ResponseEntity<List<QCControlLot>> getActiveControlLots(@RequestParam Integer testId,
            @RequestParam Integer instrumentId) {
        try {
            List<QCControlLot> controlLots = controlLotService.getActiveControlLots(testId, instrumentId);
            return ResponseEntity.ok(controlLots);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getActiveControlLots", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific control lot by ID. GET /rest/qc/controlLot/{id}
     */
    @GetMapping("/controlLot/{id}")
    public ResponseEntity<QCControlLot> getControlLot(@PathVariable("id") String id) {
        try {
            QCControlLot controlLot = controlLotService.get(id);
            if (controlLot != null) {
                return ResponseEntity.ok(controlLot);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getControlLot", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a control lot by lot number. GET
     * /rest/qc/controlLot/byLotNumber/{lotNumber}
     */
    @GetMapping("/controlLot/byLotNumber/{lotNumber}")
    public ResponseEntity<QCControlLot> getControlLotByLotNumber(@PathVariable("lotNumber") String lotNumber) {
        try {
            QCControlLot controlLot = controlLotService.getControlLotByLotNumber(lotNumber);
            if (controlLot != null) {
                return ResponseEntity.ok(controlLot);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getControlLotByLotNumber", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create or update a control lot. POST /rest/qc/controlLot
     */
    @PostMapping("/controlLot")
    public ResponseEntity<Object> saveControlLot(@RequestBody @Valid QCControlLotForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageUtil.getMessage("error.validation.controlLot"));
        }

        try {
            QCControlLot controlLot;
            boolean isNew = StringUtils.isBlank(form.getId()) || "0".equals(form.getId());

            if (isNew) {
                // Create new control lot
                controlLot = new QCControlLot();
                PropertyUtils.copyProperties(controlLot, form);
                controlLot = controlLotService.createControlLot(controlLot);
            } else {
                // Update existing control lot
                controlLot = controlLotService.get(form.getId());
                if (controlLot == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(MessageUtil.getMessage("error.notFound.controlLot"));
                }
                PropertyUtils.copyProperties(controlLot, form);
                controlLotService.update(controlLot);
            }

            return ResponseEntity.ok(controlLot);

        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "saveControlLot", "Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "saveControlLot", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.save.controlLot"));
        }
    }

    /**
     * Activate a control lot (transition from ESTABLISHMENT to ACTIVE). PUT
     * /rest/qc/controlLot/{id}/activate
     */
    @PutMapping("/controlLot/{id}/activate")
    public ResponseEntity<Object> activateControlLot(@PathVariable("id") String id) {
        try {
            QCControlLot controlLot = controlLotService.activateControlLot(id);
            if (controlLot != null) {
                return ResponseEntity.ok(controlLot);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "activateControlLot", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.activate.controlLot"));
        }
    }

    /**
     * Deactivate a control lot (mark as EXPIRED). PUT
     * /rest/qc/controlLot/{id}/deactivate
     */
    @PutMapping("/controlLot/{id}/deactivate")
    public ResponseEntity<Object> deactivateControlLot(@PathVariable("id") String id) {
        try {
            QCControlLot controlLot = controlLotService.deactivateControlLot(id);
            if (controlLot != null) {
                return ResponseEntity.ok(controlLot);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "deactivateControlLot", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.deactivate.controlLot"));
        }
    }

    /**
     * Get the latest statistics for a control lot. GET
     * /rest/qc/controlLot/{id}/statistics
     */
    @GetMapping("/controlLot/{id}/statistics")
    public ResponseEntity<QCStatistics> getLatestStatistics(@PathVariable("id") String id) {
        try {
            QCStatistics statistics = statisticsService.getLatestStatistics(id);
            if (statistics != null) {
                return ResponseEntity.ok(statistics);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getLatestStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Calculate initial runs statistics for a control lot. POST
     * /rest/qc/controlLot/{id}/statistics/initialRuns?requiredRuns={requiredRuns}
     */
    @PostMapping("/controlLot/{id}/statistics/initialRuns")
    public ResponseEntity<Object> calculateInitialRunsStatistics(@PathVariable("id") String id,
            @RequestParam Integer requiredRuns) {
        try {
            QCStatistics statistics = statisticsService.calculateInitialRunsStatistics(id, requiredRuns);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "calculateInitialRunsStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "calculateInitialRunsStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.calculate.statistics"));
        }
    }

    /**
     * Calculate rolling window statistics for a control lot. POST
     * /rest/qc/controlLot/{id}/statistics/rolling?windowSize={windowSize}
     */
    @PostMapping("/controlLot/{id}/statistics/rolling")
    public ResponseEntity<Object> calculateRollingStatistics(@PathVariable("id") String id,
            @RequestParam Integer windowSize) {
        try {
            QCStatistics statistics = statisticsService.calculateRollingStatistics(id, windowSize);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "calculateRollingStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "calculateRollingStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.calculate.statistics"));
        }
    }

    // ==================== Westgard Rule Configuration Endpoints (T073)
    // ====================

    /**
     * Get all rule configurations for a specific test and instrument. GET
     * /rest/qc/ruleConfig?testId={testId}&instrumentId={instrumentId}
     */
    @GetMapping("/ruleConfig")
    public ResponseEntity<List<WestgardRuleConfig>> getRuleConfigurations(@RequestParam Integer testId,
            @RequestParam Integer instrumentId) {
        try {
            List<WestgardRuleConfig> configs = ruleConfigService.findByTestAndInstrument(testId, instrumentId);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getRuleConfigurations", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get only enabled rule configurations for a test and instrument. GET
     * /rest/qc/ruleConfig/enabled?testId={testId}&instrumentId={instrumentId}
     */
    @GetMapping("/ruleConfig/enabled")
    public ResponseEntity<List<WestgardRuleConfig>> getEnabledRuleConfigurations(@RequestParam Integer testId,
            @RequestParam Integer instrumentId) {
        try {
            List<WestgardRuleConfig> configs = ruleConfigService.findEnabledByTestAndInstrument(testId, instrumentId);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getEnabledRuleConfigurations", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update a single rule configuration. PUT /rest/qc/ruleConfig/{id}
     */
    @PutMapping("/ruleConfig/{id}")
    public ResponseEntity<Object> updateRuleConfiguration(@PathVariable("id") String id,
            @RequestBody @Valid WestgardRuleConfigForm.RuleConfigDTO ruleConfigDTO, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageUtil.getMessage("error.validation.ruleConfig"));
        }

        try {
            WestgardRuleConfig existing = ruleConfigService.get(id);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Update fields from DTO
            if (ruleConfigDTO.getEnabled() != null) {
                existing.setEnabled(ruleConfigDTO.getEnabled());
            }
            if (ruleConfigDTO.getRequiresCorrectiveAction() != null) {
                existing.setRequiresCorrectiveAction(ruleConfigDTO.getRequiresCorrectiveAction());
            }

            WestgardRuleConfig updated = ruleConfigService.updateRuleConfig(existing);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "updateRuleConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "updateRuleConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.update.ruleConfig"));
        }
    }

    /**
     * Apply a preset configuration (BASIC, STANDARD, COMPREHENSIVE). POST
     * /rest/qc/ruleConfig/preset?testId={testId}&instrumentId={instrumentId}&preset={preset}
     */
    @PostMapping("/ruleConfig/preset")
    public ResponseEntity<Object> applyPresetConfiguration(@RequestParam Integer testId,
            @RequestParam Integer instrumentId, @RequestParam String preset) {
        try {
            List<WestgardRuleConfig> updatedConfigs = ruleConfigService.applyPreset(testId, instrumentId, preset);
            return ResponseEntity.ok(updatedConfigs);
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "applyPresetConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "applyPresetConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.apply.preset"));
        }
    }

    /**
     * Create default rule configurations for a new test-instrument combination.
     * POST /rest/qc/ruleConfig/defaults?testId={testId}&instrumentId={instrumentId}
     */
    @PostMapping("/ruleConfig/defaults")
    public ResponseEntity<Object> createDefaultRuleConfigurations(@RequestParam Integer testId,
            @RequestParam Integer instrumentId) {
        try {
            // Check if configs already exist
            List<WestgardRuleConfig> existing = ruleConfigService.findByTestAndInstrument(testId, instrumentId);
            if (!existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Rule configurations already exist for this test-instrument combination. "
                                + "Use preset endpoint to apply a different configuration.");
            }

            List<WestgardRuleConfig> defaults = ruleConfigService.createDefaultConfig(testId, instrumentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(defaults);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "createDefaultRuleConfigurations", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageUtil.getMessage("error.create.ruleConfig"));
        }
    }

    /**
     * Validate the current rule configuration. POST
     * /rest/qc/ruleConfig/validate?testId={testId}&instrumentId={instrumentId}
     */
    @PostMapping("/ruleConfig/validate")
    public ResponseEntity<Object> validateRuleConfiguration(@RequestParam Integer testId,
            @RequestParam Integer instrumentId) {
        try {
            List<WestgardRuleConfig> configs = ruleConfigService.findByTestAndInstrument(testId, instrumentId);
            ruleConfigService.validateRuleConfig(configs);
            return ResponseEntity.ok().body("Rule configuration is valid");
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn("QCRestController", "validateRuleConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "validateRuleConfiguration", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== Dashboard Endpoints (T120/T121) ====================

    /**
     * Get dashboard summary with aggregate violation counts. GET
     * /rest/qc/dashboard/summary
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        try {
            DashboardSummary summary = dashboardService.getDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getDashboardSummary", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get compliance status for all instruments with unresolved violations. GET
     * /rest/qc/dashboard/instruments
     */
    @GetMapping("/dashboard/instruments")
    public ResponseEntity<List<InstrumentComplianceStatus>> getAllInstrumentComplianceStatus() {
        try {
            List<InstrumentComplianceStatus> statuses = dashboardService.getAllInstrumentComplianceStatus();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getAllInstrumentComplianceStatus", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get compliance status for a specific instrument. GET
     * /rest/qc/dashboard/instruments/{instrumentId}
     */
    @GetMapping("/dashboard/instruments/{instrumentId}")
    public ResponseEntity<InstrumentComplianceStatus> getInstrumentComplianceStatus(
            @PathVariable("instrumentId") Integer instrumentId) {
        try {
            InstrumentComplianceStatus status = dashboardService.getInstrumentComplianceStatus(instrumentId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            LogEvent.logError("QCRestController", "getInstrumentComplianceStatus", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
