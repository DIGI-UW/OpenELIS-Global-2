package org.openelisglobal.sample.service;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TbSampleServiceImplThreadSafetyTest {

    private static final Set<String> FORBIDDEN_STATE_FIELDS = Set.of("sampleId", "patientId", "sampleItemId");

    @Test
    public void testTbSampleServiceImplHasNoPerRequestStateFields() {
        List<Field> stateFields = Arrays.stream(TbSampleServiceImpl.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> FORBIDDEN_STATE_FIELDS.contains(f.getName())).collect(Collectors.toList());

        assertTrue("TbSampleServiceImpl singleton bean must not retain per-request state fields: " + stateFields,
                stateFields.isEmpty());
    }

    @Test
    public void testTbSampleServiceImplOnlyContainsInjectedDependencies() {
        List<Field> nonInjectedInstanceFields = Arrays.stream(TbSampleServiceImpl.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).filter(f -> !f.isAnnotationPresent(Autowired.class))
                .collect(Collectors.toList());

        assertTrue("All non-static fields in singleton TbSampleServiceImpl should be Spring @Autowired dependencies: "
                + nonInjectedInstanceFields, nonInjectedInstanceFields.isEmpty());
    }
}
