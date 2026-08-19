package org.openelisglobal.analyzer.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import org.junit.Test;

public class AnalyzerProfileBindingTest {

    @Test
    public void analyzerReferencesOneSharedProfileRevisionWithoutCopiedProfilePayload() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setProfileId("sysmex-xn");
        binding.setProfileRevision(3);
        binding.setProfileFingerprint("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        Analyzer analyzer = new Analyzer();
        analyzer.setProfileBinding(binding);

        assertSame(binding, analyzer.getProfileBinding());
        assertEquals("sysmex-xn", binding.getProfileId());
        assertEquals(3, binding.getProfileRevision());
        assertFalse(Arrays.stream(AnalyzerProfileBinding.class.getDeclaredFields())
                .map(field -> field.getName().toLowerCase())
                .anyMatch(name -> name.contains("json") || name.contains("snapshot") || name.contains("payload")));
    }
}
