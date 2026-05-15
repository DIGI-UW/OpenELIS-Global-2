package org.openelisglobal.notebook.service;

import java.util.Map;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Builds pathologist attestation fields from the logged-in system user
 * (given name + father's name stored in {@link SystemUser#getLastName()}).
 */
public final class PathologyUserAttestationUtil {

    private PathologyUserAttestationUtil() {
    }

    public static String resolveLegalName(SystemUserService systemUserService, String sysUserId) {
        if (systemUserService == null || sysUserId == null || sysUserId.isBlank()) {
            return "";
        }
        SystemUser user = systemUserService.get(sysUserId);
        if (user == null) {
            return "";
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String fatherName = user.getLastName() != null ? user.getLastName().trim() : "";
        if (!firstName.isEmpty() && !fatherName.isEmpty()) {
            return firstName + " " + fatherName;
        }
        if (!firstName.isEmpty()) {
            return firstName;
        }
        if (!fatherName.isEmpty()) {
            return fatherName;
        }
        String login = user.getLoginName();
        return login != null ? login.trim() : "";
    }

    /**
     * When a sample is verified or the report is finalized, fill empty pathologist
     * name/signature fields from the current user profile.
     */
    public static void applyPathologistFieldsIfVerifying(Map<String, Object> data, SystemUserService systemUserService,
            String sysUserId) {
        if (data == null) {
            return;
        }
        boolean verifying = Boolean.TRUE.equals(data.get("verifiedByPathologist"));
        boolean finalized = Boolean.TRUE.equals(data.get("reportFinalized"));
        if (!verifying && !finalized) {
            return;
        }
        String legalName = resolveLegalName(systemUserService, sysUserId);
        if (legalName.isEmpty()) {
            return;
        }
        putIfBlank(data, "verifyingPathologistName", legalName);
        putIfBlank(data, "pathologistSignature", legalName);
        putIfBlank(data, "diagnosingPathologist", legalName);
    }

    private static void putIfBlank(Map<String, Object> data, String key, String value) {
        Object existing = data.get(key);
        if (existing == null || String.valueOf(existing).trim().isEmpty()) {
            data.put(key, value);
        }
    }
}
