package org.openelisglobal.common.rest.provider.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.common.rest.provider.bean.TestDisplayBean.ComponentBean;

/**
 * A test does not have one result type any more — its components do, and they
 * can differ. Which types a test can satisfy decides whether it appears in a
 * rule builder's search at all, so reading the primary component's type as the
 * whole test's hid every test whose numeric parts sit under a coded primary.
 */
public class TestDisplayBeanTest {

    private ComponentBean component(String id, String label, String resultType, boolean primary) {
        return new ComponentBean(id, label, resultType, primary);
    }

    @Test
    public void singleComponentNumericTest_satisfiesNumeric() {
        TestDisplayBean bean = new TestDisplayBean("1", "Glucose", "N");
        bean.setComponents(Collections.singletonList(component("c1", "Glucose", "N", true)));

        assertTrue(bean.getResultTypes().contains("N"));
        assertEquals(1, bean.getComponents().size());
    }

    @Test
    public void singleComponentNonNumericTest_doesNotSatisfyNumeric() {
        TestDisplayBean bean = new TestDisplayBean("2", "HIV", "D");
        bean.setComponents(Collections.singletonList(component("c1", "HIV", "D", true)));

        assertFalse(bean.getResultTypes().contains("N"));
        assertTrue(bean.getResultTypes().contains("D"));
    }

    @Test
    public void codedPrimaryWithNumericSecondary_isStillFoundByANumericSearch() {
        // The COVID-19 PCR shape: a coded interpretation reported beside two
        // numeric Ct values. Asking the test returns "D" and hides it.
        TestDisplayBean bean = new TestDisplayBean("300", "COVID-19 PCR", "D");
        bean.setComponents(Arrays.asList(component("c-pcr", "Interpretation", "D", true),
                component("c-vl", "Viral Load", "N", false), component("c-ct", "Ct Value", "N", false)));

        assertTrue("a test with any numeric component belongs in the numeric search",
                bean.getResultTypes().contains("N"));
        assertTrue(bean.getResultTypes().contains("D"));
    }

    @Test
    public void numericPrimaryWithTextSecondary_satisfiesBoth() {
        TestDisplayBean bean = new TestDisplayBean("4", "Culture", "N");
        bean.setComponents(
                Arrays.asList(component("c1", "Count", "N", true), component("c2", "Notes", "A", false)));

        assertTrue(bean.getResultTypes().contains("N"));
        assertTrue(bean.getResultTypes().contains("A"));
    }

    @Test
    public void severalNumericComponents_areAllOffered() {
        TestDisplayBean bean = new TestDisplayBean("300", "COVID-19 PCR", "D");
        bean.setComponents(Arrays.asList(component("c-pcr", "Interpretation", "D", true),
                component("c-vl", "Viral Load", "N", false), component("c-ct", "Ct Value", "N", false)));

        long numeric = bean.getComponents().stream().filter(c -> "N".equals(c.getResultType())).count();
        assertEquals("both numeric components are selectable, the coded one is not", 2, numeric);
    }

    @Test
    public void componentWithoutItsOwnType_reportsTheTestsType() {
        // What a legacy single-component test looks like: the component carries
        // no type of its own and inherits the test's.
        TestDisplayBean bean = new TestDisplayBean("5", "Legacy", "N");
        bean.setComponents(Collections.singletonList(component("c1", "Legacy", null, true)));

        assertTrue(bean.getResultTypes().contains("N"));
    }

    @Test
    public void testWithNoComponents_keepsItsOwnType() {
        // Nothing to read components from, so the test-level type stands —
        // the behaviour every caller had before components existed.
        TestDisplayBean bean = new TestDisplayBean("6", "Uncomponented", "N");
        bean.setComponents(new ArrayList<>());

        assertTrue(bean.getResultTypes().contains("N"));
        assertTrue(bean.getComponents().isEmpty());
    }

    @Test
    public void resultTypesAreRecomputedWhenComponentsChange() {
        TestDisplayBean bean = new TestDisplayBean("7", "Changing", "D");
        assertFalse(bean.getResultTypes().contains("N"));

        bean.setComponents(Collections.singletonList(component("c1", "Now numeric", "N", true)));

        assertTrue(bean.getResultTypes().contains("N"));
        assertFalse("the stale test-level type must not linger", bean.getResultTypes().contains("D"));
    }
}
