package org.openelisglobal.containertype.form;

import java.math.BigDecimal;
import java.util.List;

/**
 * The containers proposed for one sample (FR-B24, B25): one entry per container
 * needed, with the tests it serves and the panels they were ordered through.
 * Containers the tests share come first; a container drawn for tests that need
 * their own is marked, as no other test may join it. Tests no container is
 * proposed for are listed apart (FR-G8). The choices are the active clinical
 * containers yielding the sample's type, and a container is required on the
 * sample when anything was proposed.
 */
public record ContainerProposalView(String key, List<Proposed> proposed, List<String> unproposedTestIds,
        List<Choice> choices, boolean containerRequired) {

    public record Proposed(Long containerTypeId, String name, String capColourHex, BigDecimal nominalVolume,
            String volumeUnit, int count, List<String> testIds, List<String> testNames, List<String> panelIds,
            boolean ownContainer) {
    }

    public record Choice(Long id, String name, String capColourHex, BigDecimal nominalVolume, String volumeUnit) {
    }
}
