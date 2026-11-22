package org.openelisglobal.common.domain.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class DomainRestControllerTest {

    @Test
    public void listDomains_returnsEveryDomainWithItsLabelKey() {
        List<DomainRestController.DomainDto> domains = new DomainRestController().listDomains();

        List<String> ids = domains.stream().map(d -> d.id).collect(Collectors.toList());
        assertEquals(List.of("CLINICAL", "ENVIRONMENTAL", "VECTOR"), ids);
        assertTrue(domains.stream().allMatch(d -> d.labelKey != null && d.labelKey.startsWith("label.domain.")));
    }
}
