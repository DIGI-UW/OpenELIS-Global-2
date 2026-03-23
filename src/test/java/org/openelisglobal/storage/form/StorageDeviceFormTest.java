package org.openelisglobal.storage.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.BeforeClass;
import org.junit.Test;

public class StorageDeviceFormTest {

    private static Validator validator;

    @BeforeClass
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();
        validator = factory.getValidator();
    }

    private StorageDeviceForm buildBaseForm() {
        StorageDeviceForm form = new StorageDeviceForm();
        form.setName("Device");
        form.setType("freezer");
        return form;
    }

    @Test
    public void ipAddress_AllowsCompressedIpv6() {
        String[] validAddresses = { "::1", "2001:db8::1", "fe80::1", "192.168.1.10" };

        for (String addr : validAddresses) {
            StorageDeviceForm form = buildBaseForm();
            form.setIpAddress(addr);
            Set<ConstraintViolation<StorageDeviceForm>> violations = validator.validate(form);
            assertTrue("Expected no violations for address: " + addr, violations.isEmpty());
        }
    }

    @Test
    public void ipAddress_RejectsInvalidAddresses() {
        String[] invalidAddresses = { "999.999.999.999", "gggg::1", "1234:5678:90ab:cdef:1234:5678:90ab:cdef:1234" };

        for (String addr : invalidAddresses) {
            StorageDeviceForm form = buildBaseForm();
            form.setIpAddress(addr);
            Set<ConstraintViolation<StorageDeviceForm>> violations = validator.validate(form);
            assertEquals("Expected exactly one violation for address: " + addr, 1, violations.size());
            String message = violations.iterator().next().getMessage();
            assertTrue("Unexpected message for address: " + addr, message.contains("IP address must be valid"));
        }
    }
}
