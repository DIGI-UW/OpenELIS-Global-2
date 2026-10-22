package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EgressTest {

    @Test
    void onlyDeclaredHostsAreCalled() {
        List<String> before = Egress.allowed();
        try {
            Egress.allow(List.of("openrouter.ai", "host.containers.internal:4190"));
            assertTrue(Egress.permits("https://openrouter.ai/api/v1/chat/completions"));
            assertTrue(Egress.permits("http://host.containers.internal:4190/ask"));
            assertFalse(Egress.permits("http://host.containers.internal:22/ask"));
            assertFalse(Egress.permits("https://evil.example.org/api/v1/chat/completions"));
            assertFalse(Egress.permits("https://openrouter.ai.evil.example.org/x"));
            assertFalse(Egress.permits("file:///etc/passwd"));
        } finally {
            Egress.allow(before);
        }
    }

    @Test
    void thePackagedManifestDeclaresThePublicProviders() {
        assertTrue(Egress.allowed().contains("openrouter.ai"), Egress.allowed().toString());
    }
}
