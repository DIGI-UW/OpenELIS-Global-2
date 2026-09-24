package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;

import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;

public class AnalyzerCutoverChecksumTest {

    @Test
    public void guardCorrectionKeepsThePublishedChangesetChecksum() throws Exception {
        var changelog = new XMLChangeLogSAXParser().parse("liquibase/3.5.x.x/098-remove-superseded-analyzer-schema.xml",
                new ChangeLogParameters(), new ClassLoaderResourceAccessor());
        assertEquals("8:174283a444a158e0c81e074710050e03",
                changelog.getChangeSets().get(0).generateCheckSum().toString());
    }
}
