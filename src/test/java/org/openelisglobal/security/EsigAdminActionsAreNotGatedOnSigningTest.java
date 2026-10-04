package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Administering who may sign is not itself a signing action.
 *
 * <p>
 * {@code PRIV_ESIG_USE} is held by every role that signs a record - Results,
 * Validation, Pathologist, Cytopathologist. Gating {@code revokeCertification}
 * on it would therefore let any signer strip another signer's first-use
 * certification and lock them out of signing, which is an administrative act,
 * not a peer one. {@code getAllCertifications} lists the same administrative
 * view.
 *
 * <p>
 * Both sit on {@code PRIV_SYSTEM_USER_MANAGE} instead, which is granted to no
 * role and so resolves to admin only, matching the user administration screens
 * these endpoints belong with.
 */
public class EsigAdminActionsAreNotGatedOnSigningTest {

    private static final Path SERVICE = Paths
            .get("src/main/java/org/openelisglobal/esig/service/ElectronicSignatureService.java");

    private static String gateAbove(String body, String signature) {
        int at = body.indexOf(signature);
        assertTrue(signature + " not found in " + SERVICE, at >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", at);
        assertTrue(signature + " must carry a @PreAuthorize", gate >= 0);
        return body.substring(gate, at);
    }

    @Test
    public void adminCertificationActionsRequireUserAdministration() throws IOException {
        String body = new String(Files.readAllBytes(SERVICE), StandardCharsets.UTF_8);

        for (String signature : new String[] { "void revokeCertification(String username);",
                "List<EsigFirstUseCertification> getAllCertifications();" }) {
            String gate = gateAbove(body, signature);
            assertTrue(
                    signature + " must NOT be gated on PRIV_ESIG_USE: every signing role holds it, so"
                            + " that lets one signer revoke another's certification. Found: " + gate.trim(),
                    !gate.contains("PRIV_ESIG_USE"));
            assertTrue(
                    signature + " must require PRIV_SYSTEM_USER_MANAGE, the user administration"
                            + " privilege no role is granted. Found: " + gate.trim(),
                    gate.contains("PRIV_SYSTEM_USER_MANAGE"));
        }
    }

    @Test
    public void signingItselfStaysOnTheSigningPrivilege() throws IOException {
        String body = new String(Files.readAllBytes(SERVICE), StandardCharsets.UTF_8);
        String gate = gateAbove(body, "ElectronicSignature executeSignature(");
        assertTrue("executeSignature must stay on PRIV_ESIG_USE; tightening it would stop the signing roles"
                + " signing at all. Found: " + gate.trim(), gate.contains("PRIV_ESIG_USE"));
    }
}
