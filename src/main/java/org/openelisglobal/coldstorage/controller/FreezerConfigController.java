package org.openelisglobal.coldstorage.controller;

import jakarta.validation.Valid;
import org.openelisglobal.coldstorage.service.SystemConfigService;
import org.openelisglobal.coldstorage.service.SystemConfigService.SystemConfigDTO;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({ "/rest/coldstorage", "/rest/freezer-monitoring" })
public class FreezerConfigController extends BaseRestController {

    private final SystemConfigService systemConfigService;

    public FreezerConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/system-config")
    public ResponseEntity<SystemConfigDTO> getSystemConfig() {
        SystemConfigDTO config = systemConfigService.getGlobalConfig();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/system-config")
    public ResponseEntity<SystemConfigDTO> saveSystemConfig(@RequestBody @Valid SystemConfigDTO config) {
        SystemConfigDTO saved = systemConfigService.saveConfig(config);
        return ResponseEntity.ok(saved);
    }
}
