package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerProfileService;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for analyzer profile library. Endpoints per API contract:
 * GET/POST /profiles, GET/PUT/DELETE /profiles/{id}, profile-apply. MVP: all
 * endpoints require GLOBAL_ADMIN; tiered RBAC (e.g. read for lab roles)
 * deferred to post-MVP.
 */
@RestController
@RequestMapping("/rest/analyzer")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AnalyzerProfileRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerProfileRestController.class);

    @Autowired
    private AnalyzerProfileService analyzerProfileService;

    @GetMapping("/profiles")
    public ResponseEntity<List<Map<String, Object>>> listProfiles(@RequestParam(required = false) String source,
            @RequestParam(required = false) String profileMetaId) {
        try {
            List<AnalyzerProfile> profiles;
            if (profileMetaId != null && !profileMetaId.isEmpty()) {
                profiles = analyzerProfileService.listByMetaId(profileMetaId);
            } else if (source != null && !source.isEmpty()) {
                profiles = analyzerProfileService.listBySource(source);
            } else {
                profiles = analyzerProfileService.getAll();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (AnalyzerProfile p : profiles) {
                result.add(profileToMap(p));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error listing profiles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String profileId) {
        try {
            AnalyzerProfile profile = analyzerProfileService.get(profileId);
            Map<String, Object> map = profileToMap(profile);
            map.put("profileJson", profile.getProfileJson());
            return ResponseEntity.ok(map);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AnalyzerControllerHelper.wrapError("Profile not found: " + profileId));
        } catch (Exception e) {
            logger.error("Error getting profile", e);
            // API error payloads are developer-facing; frontend maps to localized UI text.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    @PostMapping("/profiles")
    public ResponseEntity<Map<String, Object>> importProfile(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) request.get("profile");
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AnalyzerControllerHelper.wrapError("profile is required"));
            }
            String sysUserId = getSysUserId(httpRequest);
            String id = analyzerProfileService.importProfile(profile, "SITE", sysUserId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", id);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
            }
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error importing profile", e);
            // API error payloads are developer-facing; frontend maps to localized UI text.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    @PutMapping("/profiles/{profileId}")
    public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable String profileId,
            @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            AnalyzerProfile profile = analyzerProfileService.get(profileId);
            if (!Boolean.TRUE.equals(profile.getIsMutable())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(AnalyzerControllerHelper.wrapError("Built-in profile cannot be modified"));
            }
            if (request.containsKey("displayName")) {
                profile.setDisplayName((String) request.get("displayName"));
            }
            profile.setUpdatedBy(getSysUserId(httpRequest));
            profile.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            analyzerProfileService.update(profile);
            if (request.containsKey("isLatest") && Boolean.TRUE.equals(request.get("isLatest"))) {
                analyzerProfileService.setDesignatedLatest(profileId);
            }
            profile = analyzerProfileService.get(profileId);
            return ResponseEntity.ok(profileToMap(profile));
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AnalyzerControllerHelper.wrapError("Profile not found"));
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            // API error payloads are developer-facing; frontend maps to localized UI text.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    @DeleteMapping("/profiles/{profileId}")
    public ResponseEntity<?> deleteProfile(@PathVariable String profileId) {
        try {
            AnalyzerProfile profile = analyzerProfileService.get(profileId);
            if (!Boolean.TRUE.equals(profile.getIsMutable())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (analyzerProfileService.hasApplications(profileId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(AnalyzerControllerHelper
                        .wrapError("Profile cannot be deleted: one or more analyzers have applied this profile"));
            }
            analyzerProfileService.delete(profile);
            return ResponseEntity.noContent().build();
        } catch (org.hibernate.ObjectNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/analyzers/{analyzerId}/profile-apply")
    public ResponseEntity<Map<String, Object>> applyProfile(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            String profileId = (String) request.get("profileId");
            if (profileId == null || profileId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AnalyzerControllerHelper.wrapError("profileId is required"));
            }
            analyzerProfileService.applyProfileToAnalyzer(analyzerId, profileId, getSysUserId(httpRequest));
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error applying profile", e);
            // API error payloads are developer-facing; frontend maps to localized UI text.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    private Map<String, Object> profileToMap(AnalyzerProfile p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("profileMetaId", p.getProfileMetaId());
        map.put("profileMetaVersion", p.getProfileMetaVersion());
        map.put("displayName", p.getDisplayName());
        map.put("source", p.getSource());
        map.put("isLatest", p.getIsLatest());
        map.put("isMutable", p.getIsMutable());
        return map;
    }
}
