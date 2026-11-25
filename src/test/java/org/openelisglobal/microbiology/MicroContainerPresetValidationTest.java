package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.labelpreset.form.LabelPresetForm;

public class MicroContainerPresetValidationTest {
    @Test
    public void containerOnlyScopeAndQuantityBoundariesAreValidated() {
        var form = new LabelPresetForm();
        form.setPrintsPerSample(false);
        form.setPrintsPerOrder(false);
        assertFalse(form.isAtLeastOneScopeSelected());
        form.setPrintsPerContainer(true);
        assertTrue(form.isAtLeastOneScopeSelected());
        assertTrue(form.isMaxPerContainerGteDefault());
        form.setDefaultPerContainer(11);
        assertFalse(form.isMaxPerContainerGteDefault());
        form.setDefaultPerContainer(-1);
        assertFalse(form.isMaxPerContainerGteDefault());
        form.setDefaultPerContainer(null);
        assertFalse(form.isMaxPerContainerGteDefault());
        form.setDefaultPerContainer(10);
        assertTrue(form.isMaxPerContainerGteDefault());
    }
}
