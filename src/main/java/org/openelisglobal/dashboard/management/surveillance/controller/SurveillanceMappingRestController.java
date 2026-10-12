package org.openelisglobal.dashboard.management.surveillance.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingService;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingView;
import org.openelisglobal.qa.security.QaPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest/dashboards/management/surveillance-mappings")
@PreAuthorize(QaPermissions.MANAGE_QI)
public class SurveillanceMappingRestController extends BaseRestController {

    private static final long MAX_BYTES = 1_000_000;

    @Autowired
    private SurveillanceMappingService surveillanceMappingService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SurveillanceMappingView> list() {
        return surveillanceMappingService.getAllViews();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, HttpServletRequest request)
            throws IOException {
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 1MB)"));
        }
        String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        // Excel prefixes its UTF-8 CSVs with a byte-order mark, which would hide the
        // first header.
        if (csv.startsWith("﻿")) {
            csv = csv.substring(1);
        }
        try {
            return ResponseEntity.ok(surveillanceMappingService.importCsv(new StringReader(csv),
                    file.getOriginalFilename(), getSysUserId(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
